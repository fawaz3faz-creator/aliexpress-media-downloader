package com.fawaz.aliexpressdownloader.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.fawaz.aliexpressdownloader.data.api.AliExpressApiService
import com.fawaz.aliexpressdownloader.model.MediaItem
import com.fawaz.aliexpressdownloader.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import retrofit2.HttpException
import java.io.File
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit

class AliExpressRepository(
    private val api: AliExpressApiService,
    private val okHttpClient: OkHttpClient
) {

    suspend fun fetchMedia(rawUrl: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val resolvedUrl = resolveRedirectUrl(rawUrl)
        val response = api.getPage(resolvedUrl)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val html = response.body() ?: ""
        val parser = AliExpressMediaParser()
        parser.parse(html, resolvedUrl)
    }

    suspend fun resolveRedirectUrl(rawUrl: String): String = withContext(Dispatchers.IO) {
        val request = okhttp3.Request.Builder().url(rawUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            response.request.url.toString()
        }
    }

    companion object {
        fun createClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}

class AliExpressMediaParser {
    fun parse(html: String, sourceUrl: String): List<MediaItem> {
        val urls = linkedSetOf<String>()
        val scriptText = html

        val regex = Regex(
            "https?://[^\\s\"'<>]+(?:\\.(?:jpe?g|png|webp|gif|bmp|avif|mp4|m3u8|webm|mpd))(?:\\?[^\\s\"'<>]+)?",
            setOf(RegexOption.IGNORE_CASE)
        )

        regex.findAll(scriptText).forEach { match ->
            val clean = cleanUrl(match.value)
            if (clean.isNotBlank()) urls += clean
        }

        val doc = Jsoup.parse(html)
        doc.select("img, source, video").forEach { element ->
            val src = element.attr("src").ifBlank { element.attr("data-src") }
            val srcset = element.attr("srcset")
            if (src.isNotBlank()) urls += cleanUrl(src)
            if (srcset.isNotBlank()) {
                srcset.split(",").forEach { part ->
                    val candidate = part.trim().split(" ").firstOrNull() ?: ""
                    if (candidate.isNotBlank()) urls += cleanUrl(candidate)
                }
            }
        }

        return urls.mapIndexed { index, url ->
            val bestUrl = boostToHighestQuality(url)
            val mediaType = if (bestUrl.lowercase(Locale.ROOT).contains(".mp4") || bestUrl.lowercase(Locale.ROOT)
                    .contains(".m3u8") || bestUrl.lowercase(Locale.ROOT).contains(".webm")
            )
                MediaType.VIDEO else MediaType.IMAGE

            MediaItem(
                id = "media_${sourceUrl.hashCode()}_${index}",
                url = bestUrl,
                mediaType = mediaType,
                title = "Media ${index + 1}",
                source = "AliExpress",
                quality = resolveQuality(bestUrl),
                selected = false
            )
        }.distinctBy { it.url }
    }

    private fun cleanUrl(raw: String): String {
        val value = raw.trim()
            .replace("\\u003d", "=")
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .trimEnd(',', '"', '\'', ')', ']', '}')

        return if (value.startsWith("http://") || value.startsWith("https://")) {
            value
        } else {
            ""
        }
    }

    private fun boostToHighestQuality(rawUrl: String): String {
        var out = rawUrl
        out = out.replace(Regex("_\\d+x\\d+"), "")
        out = out.replace(Regex("_640x640|_400x400|_300x300|_220x220|_120x120"), "")
        out = out.replace(Regex("(?:\\?p[0-9]+)?$"), "")
        return out
    }

    private fun resolveQuality(rawUrl: String): String {
        return when {
            rawUrl.contains("mp4") || rawUrl.contains("m3u8") || rawUrl.contains("webm") -> "HD / Original"
            rawUrl.contains("_1000") || rawUrl.contains("_1200") || rawUrl.contains("_1500") -> "HD / Original"
            rawUrl.contains("_640") || rawUrl.contains("_400") -> "HD"
            else -> "Original"
        }
    }
}

object GalleryMediaSaver {
    fun download(context: Context, mediaItem: MediaItem): Uri? {
        val client = OkHttpClient.Builder().build()
        val request = okhttp3.Request.Builder().url(mediaItem.url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val bytes = response.body?.bytes() ?: return null
        val mimeType = if (mediaItem.mediaType == MediaType.VIDEO) "video/mp4" else "image/jpeg"
        val extension = when {
            mediaItem.mediaType == MediaType.VIDEO -> ".mp4"
            mediaItem.url.contains("png") -> ".png"
            mediaItem.url.contains("webp") -> ".webp"
            mediaItem.url.contains("gif") -> ".gif"
            else -> ".jpg"
        }

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${mediaItem.title.replace(" ", "_")}$extension")
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, if (mediaItem.mediaType == MediaType.VIDEO) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES)
            }
        }

        val uri = if (mediaItem.mediaType == MediaType.VIDEO) {
            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }

        uri ?: return null

        resolver.openOutputStream(uri)?.use { output -> output.write(bytes) }
        return uri
    }

    fun showNotification(context: Context, itemCount: Int) {
        val channelId = "ali_express_downloads"
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AliExpress Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText("${itemCount} media file(s) saved to your gallery")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager?.notify(System.currentTimeMillis().toInt(), notification)
    }
}
