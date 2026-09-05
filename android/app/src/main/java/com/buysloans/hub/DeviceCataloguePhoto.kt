package com.buysloans.hub

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private object DeviceCataloguePhotoLoader {
    private const val FAILURE_RETRY_MS = 60_000L
    private val imageCache = ConcurrentHashMap<String, ImageBitmap>()
    private val failedAt = ConcurrentHashMap<String, Long>()

    fun cached(referenceUrl: String): ImageBitmap? = imageCache[referenceUrl]

    fun hasRecentFailure(referenceUrl: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val failureTime = failedAt[referenceUrl] ?: return false
        if (nowMs - failureTime < FAILURE_RETRY_MS) return true
        failedAt.remove(referenceUrl, failureTime)
        return false
    }

    suspend fun load(referenceUrl: String): ImageBitmap? = withContext(Dispatchers.IO) {
        imageCache[referenceUrl]?.let { return@withContext it }
        if (hasRecentFailure(referenceUrl)) return@withContext null

        runCatching {
            val imageUrl = DeviceImageResolver.directImageUrl(referenceUrl)
                ?: resolveProductImage(referenceUrl)
                ?: error("No product image metadata")
            val imageConnection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "B&L-Morley/Android")
                setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            }
            try {
                val contentType = imageConnection.contentType.orEmpty().lowercase()
                if (contentType.isNotBlank() && !contentType.startsWith("image/")) {
                    error("Resolved catalogue asset is not an image")
                }
                imageConnection.inputStream.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        ?: error("Unable to decode product image")
                }
            } finally {
                imageConnection.disconnect()
            }
        }.onSuccess { bitmap ->
            imageCache[referenceUrl] = bitmap
            failedAt.remove(referenceUrl)
        }.onFailure {
            failedAt[referenceUrl] = System.currentTimeMillis()
        }.getOrNull()
    }

    private fun resolveProductImage(pageUrl: String): String? {
        val connection = (URL(pageUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) B&L-Morley/1.0")
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
        }
        return try {
            val contentType = connection.contentType.orEmpty().lowercase()
            if (contentType.startsWith("image/")) return pageUrl
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val candidates = listOf(
                Regex("""<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']og:image[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+name=[\"']twitter:image(?::src)?[\"'][^>]+content=[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+name=[\"']twitter:image(?::src)?[\"']""", RegexOption.IGNORE_CASE),
            )
            candidates.firstNotNullOfOrNull { regex -> regex.find(html)?.groupValues?.getOrNull(1) }
                ?.replace("&amp;", "&")
                ?.let { raw -> URL(URL(pageUrl), raw).toString() }
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
fun DeviceCataloguePhoto(
    brand: String,
    model: String,
    imageReferenceUrl: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit,
) {
    val reference = remember(imageReferenceUrl) { imageReferenceUrl?.trim()?.takeIf { it.isNotBlank() } }
    var bitmap by remember(reference) { mutableStateOf(reference?.let(DeviceCataloguePhotoLoader::cached)) }

    LaunchedEffect(reference) {
        if (reference == null) {
            bitmap = null
        } else if (bitmap == null && !DeviceCataloguePhotoLoader.hasRecentFailure(reference)) {
            bitmap = DeviceCataloguePhotoLoader.load(reference)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = DeviceImageResolver.accessibilityLabel(brand, model),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            fallback()
        }
    }
}
