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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

internal fun normalizeCatalogueIdentity(value: String): String =
    value.lowercase().replace(Regex("[^a-z0-9]+"), "")

internal fun cataloguePageTitleMatchesDevice(html: String, model: String, modelNumber: String?): Boolean {
    val titles = buildList {
        Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.getOrNull(1)?.let(::add)
        listOf("og:title", "twitter:title").forEach { key ->
            Regex("""<meta[^>]+(?:property|name)=[\"']$key[\"'][^>]+content=[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)?.let(::add)
            Regex("""<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+(?:property|name)=[\"']$key[\"']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)?.let(::add)
        }
    }
    if (titles.isEmpty()) return false
    val titleIdentity = normalizeCatalogueIdentity(titles.joinToString(" "))
    val modelIdentity = normalizeCatalogueIdentity(model)
    val numberIdentity = modelNumber?.let(::normalizeCatalogueIdentity).orEmpty()
    return (modelIdentity.length >= 4 && titleIdentity.contains(modelIdentity)) ||
        (numberIdentity.length >= 4 && titleIdentity.contains(numberIdentity))
}

private object DeviceCataloguePhotoLoader {
    private const val FAILURE_RETRY_MS = 60_000L
    private val imageCache = ConcurrentHashMap<String, ImageBitmap>()
    private val failedAt = ConcurrentHashMap<String, Long>()

    private fun cacheKey(referenceUrl: String, model: String, modelNumber: String?): String =
        listOf(referenceUrl, normalizeCatalogueIdentity(model), normalizeCatalogueIdentity(modelNumber.orEmpty())).joinToString("|")

    fun cached(referenceUrl: String, model: String, modelNumber: String?): ImageBitmap? =
        imageCache[cacheKey(referenceUrl, model, modelNumber)]

    fun hasRecentFailure(referenceUrl: String, model: String, modelNumber: String?, nowMs: Long = System.currentTimeMillis()): Boolean {
        val key = cacheKey(referenceUrl, model, modelNumber)
        val failureTime = failedAt[key] ?: return false
        if (nowMs - failureTime < FAILURE_RETRY_MS) return true
        failedAt.remove(key, failureTime)
        return false
    }

    suspend fun load(referenceUrl: String, model: String, modelNumber: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        val key = cacheKey(referenceUrl, model, modelNumber)
        imageCache[key]?.let { return@withContext it }
        if (hasRecentFailure(referenceUrl, model, modelNumber)) return@withContext null

        runCatching {
            val imageUrl = DeviceImageResolver.directImageUrl(referenceUrl)
                ?: resolveVerifiedProductImage(referenceUrl, model, modelNumber)
                ?: error("No verified product image metadata")
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
            imageCache[key] = bitmap
            failedAt.remove(key)
        }.onFailure {
            failedAt[key] = System.currentTimeMillis()
        }.getOrNull()
    }

    private fun decodeHtml(value: String): String = value
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

    private fun productImageFromJsonLd(html: String, model: String, modelNumber: String?): String? {
        val scripts = Regex(
            """<script[^>]+type=[\"']application/ld\+json[\"'][^>]*>(.*?)</script>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).findAll(html)
        val modelIdentity = normalizeCatalogueIdentity(model)
        val numberIdentity = normalizeCatalogueIdentity(modelNumber.orEmpty())

        fun identityMatches(value: JSONObject): Boolean {
            val identity = normalizeCatalogueIdentity(
                listOf(value.optString("name"), value.optString("model"), value.optString("sku"), value.optString("mpn"))
                    .joinToString(" "),
            )
            return (modelIdentity.length >= 4 && identity.contains(modelIdentity)) ||
                (numberIdentity.length >= 4 && identity.contains(numberIdentity))
        }

        fun imageOf(value: JSONObject): String? = when (val image = value.opt("image")) {
            is String -> image.takeIf { it.isNotBlank() }
            is JSONObject -> image.optString("url").takeIf { it.isNotBlank() }
            is JSONArray -> (0 until image.length()).firstNotNullOfOrNull { index ->
                when (val candidate = image.opt(index)) {
                    is String -> candidate.takeIf { it.isNotBlank() }
                    is JSONObject -> candidate.optString("url").takeIf { it.isNotBlank() }
                    else -> null
                }
            }
            else -> null
        }

        fun findProduct(value: Any?): String? = when (value) {
            is JSONObject -> {
                val type = value.opt("@type")
                val isProduct = when (type) {
                    is String -> type.equals("Product", true)
                    is JSONArray -> (0 until type.length()).any { type.optString(it).equals("Product", true) }
                    else -> false
                }
                if (isProduct && identityMatches(value)) imageOf(value)
                else value.keys().asSequence().firstNotNullOfOrNull { key -> findProduct(value.opt(key)) }
            }
            is JSONArray -> (0 until value.length()).firstNotNullOfOrNull { findProduct(value.opt(it)) }
            else -> null
        }

        scripts.forEach { match ->
            runCatching {
                val raw = match.groupValues[1].trim()
                val root: Any = if (raw.startsWith("[")) JSONArray(raw) else JSONObject(raw)
                findProduct(root)
            }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun resolveVerifiedProductImage(pageUrl: String, model: String, modelNumber: String?): String? {
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
            if (!cataloguePageTitleMatchesDevice(html, model, modelNumber)) return null

            val raw = productImageFromJsonLd(html, model, modelNumber) ?: listOf(
                Regex("""<meta[^>]+property=[\"']og:image(?::secure_url)?[\"'][^>]+content=[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']og:image(?::secure_url)?[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+name=[\"']twitter:image(?::src)?[\"'][^>]+content=[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+name=[\"']twitter:image(?::src)?[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+itemprop=[\"']image[\"'][^>]+content=[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+itemprop=[\"']image[\"']""", RegexOption.IGNORE_CASE),
                Regex("""<link[^>]+rel=[\"']image_src[\"'][^>]+href=[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE),
            ).firstNotNullOfOrNull { regex -> regex.find(html)?.groupValues?.getOrNull(1) }
            raw?.let(::decodeHtml)?.let { URL(URL(pageUrl), it).toString() }
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
    modelNumber: String? = null,
    fallback: @Composable () -> Unit,
) {
    val reference = remember(imageReferenceUrl) { imageReferenceUrl?.trim()?.takeIf { it.isNotBlank() } }
    var bitmap by remember(reference, model, modelNumber) {
        mutableStateOf(reference?.let { DeviceCataloguePhotoLoader.cached(it, model, modelNumber) })
    }

    LaunchedEffect(reference, model, modelNumber) {
        if (reference == null) {
            bitmap = null
        } else if (bitmap == null && !DeviceCataloguePhotoLoader.hasRecentFailure(reference, model, modelNumber)) {
            bitmap = DeviceCataloguePhotoLoader.load(reference, model, modelNumber)
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
