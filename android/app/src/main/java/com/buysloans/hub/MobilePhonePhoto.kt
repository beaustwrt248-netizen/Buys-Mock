package com.buysloans.hub

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private object MobilePhonePhotoLoader {
    private val imageCache = ConcurrentHashMap<String, ImageBitmap>()
    private val failed = ConcurrentHashMap.newKeySet<String>()

    fun cached(pageUrl: String): ImageBitmap? = imageCache[pageUrl]
    fun hasFailed(pageUrl: String): Boolean = pageUrl in failed

    suspend fun load(pageUrl: String): ImageBitmap? = withContext(Dispatchers.IO) {
        imageCache[pageUrl]?.let { return@withContext it }
        if (pageUrl in failed) return@withContext null

        runCatching {
            val imageUrl = resolveProductImage(pageUrl) ?: error("No product image metadata")
            val imageConnection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "B&L-Morley/Android")
                setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            }
            try {
                imageConnection.inputStream.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        ?: error("Unable to decode product image")
                }
            } finally {
                imageConnection.disconnect()
            }
        }.onSuccess { bitmap ->
            imageCache[pageUrl] = bitmap
        }.onFailure {
            failed += pageUrl
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
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val candidates = listOf(
                Regex("""<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']og:image[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+name=[\"']twitter:image(?::src)?[\"'][^>]+content=[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+name=[\"']twitter:image(?::src)?[\"']""", RegexOption.IGNORE_CASE)
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
fun MobilePhonePhoto(
    brand: String,
    model: String? = null,
    modifier: Modifier = Modifier,
    categoryRepresentative: Boolean = false
) {
    val pageUrl = remember(brand, model, categoryRepresentative) {
        when {
            model != null -> MobilePhonePhotoCatalog.exactModelPage(brand, model)
            categoryRepresentative -> MobilePhonePhotoCatalog.categoryPage(brand)
            else -> null
        }
    }

    var bitmap by remember(pageUrl) { mutableStateOf(pageUrl?.let(MobilePhonePhotoLoader::cached)) }
    var failed by remember(pageUrl) { mutableStateOf(pageUrl?.let(MobilePhonePhotoLoader::hasFailed) ?: true) }

    LaunchedEffect(pageUrl) {
        if (pageUrl == null) {
            failed = true
            bitmap = null
        } else if (bitmap == null && !failed) {
            bitmap = MobilePhonePhotoLoader.load(pageUrl)
            failed = bitmap == null
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = if (model == null) "$brand phone" else "$brand $model",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            PhoneBrandVisual(
                when (brand) {
                    "Apple", "Samsung", "Google", "OnePlus", "Xiaomi" -> brand
                    else -> "Other Brands"
                },
                Modifier.size(34.dp)
            )
        }
    }
}
