package com.fawaz.aliexpressdownloader.model

enum class MediaType {
    IMAGE,
    VIDEO
}

data class MediaItem(
    val id: String,
    val url: String,
    val mediaType: MediaType,
    val title: String = "Media",
    val source: String = "Product",
    val quality: String = "Original",
    val selected: Boolean = false,
    val localUri: String? = null
)
