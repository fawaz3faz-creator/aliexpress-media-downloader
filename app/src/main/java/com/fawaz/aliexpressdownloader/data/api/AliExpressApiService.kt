package com.fawaz.aliexpressdownloader.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface AliExpressApiService {
    @GET
    suspend fun getPage(@Url url: String): Response<String>

    companion object {
        fun build(context: Context): AliExpressApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://www.aliexpress.com/")
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()

            return retrofit.create(AliExpressApiService::class.java)
        }
    }
}
