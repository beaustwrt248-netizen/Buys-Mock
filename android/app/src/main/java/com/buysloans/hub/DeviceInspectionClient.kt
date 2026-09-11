package com.buysloans.hub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.roundToInt

data class DamageRegion(
    val photoIndex: Int,
    val label: String,
    val severity: String,
    val confidence: Double,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class CatalogueMatch(
    val id: String,
    val category: String,
    val brand: String,
    val family: String,
    val modelName: String,
    val modelNumber: String,
    val releaseYear: Int?,
    val storageOptions: List<String>,
    val imageReferenceUrl: String?
)

data class DeviceInspection(
    val brand: String,
    val family: String,
    val model: String,
    val modelNumber: String,
    val colour: String,
    val storage: String,
    val conditionGrade: String,
    val conditionSummary: String,
    val confidence: Double,
    val damageFlags: List<String>,
    val damageRegions: List<DamageRegion>,
    val evidence: List<String>,
    val uncertainties: List<String>,
    val nextPhotos: List<String>,
    val catalogueMatch: CatalogueMatch?,
    val qualityWarnings: List<String> = emptyList(),
    val consistencyWarnings: List<String> = emptyList(),
    val componentFindings: List<String> = emptyList()
) {
    val displayName: String
        get() = catalogueMatch?.let { match ->
            listOf(MorleyVisionPolicy.clean(match.brand), MorleyVisionPolicy.clean(match.modelName))
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }?.takeIf { it.isNotBlank() }
            ?: listOf(MorleyVisionPolicy.clean(brand), MorleyVisionPolicy.clean(model))
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "Device not verified" }

    val verifiedStorage: String
        get() = MorleyVisionPolicy.clean(storage)

    val identityVerified: Boolean
        get() = MorleyVisionPolicy.isIdentityVerified(this)

    val identityQuery: String
        get() {
            if (!identityVerified) return ""
            return listOf(
                MorleyVisionPolicy.clean(catalogueMatch?.brand).ifBlank { MorleyVisionPolicy.clean(brand) },
                MorleyVisionPolicy.clean(catalogueMatch?.modelName).ifBlank { MorleyVisionPolicy.clean(model) },
                MorleyVisionPolicy.clean(catalogueMatch?.modelNumber).ifBlank { MorleyVisionPolicy.clean(modelNumber) },
                verifiedStorage
            ).filter { it.isNotBlank() }.distinct().joinToString(" ")
        }

    val inspectionProfile: VisionInspectionProfile
        get() = MorleyVisionPolicy.inspectionProfile(catalogueMatch?.category)
}

data class VisionCapture(
    val angle: String,
    val file: File
)

data class MarketListing(
    val source: String,
    val title: String,
    val price: Double,
    val condition: String
)

data class LivePricingResult(
    val listings: List<MarketListing>,
    val usedMedian: Double?,
    val conditionAdjustedResale: Double?,
    val rejectedListingCount: Int = 0,
    val verifiedComparableCount: Int = listings.size,
    val recommendationBlockedReason: String? = null
) {
    val canUseSuggestedPrice: Boolean
        get() = recommendationBlockedReason == null && conditionAdjustedResale != null
}

object DeviceInspectionClient {
    const val REQUIRED_PHOTOS = 2
    private const val MAX_SINGLE_ENCODED_BYTES = 2_250_000
    private const val LONG_EDGE = 2560
    private const val FALLBACK_LONG_EDGE = 2048

    suspend fun inspect(context: Context, frontPhoto: File, backPhoto: File): DeviceInspection =
        inspect(
            context,
            listOf(
                VisionCapture("front", frontPhoto),
                VisionCapture("back", backPhoto)
            )
        )

    /**
     * Multi-angle entry point used by guided Morley Vision capture. Two photos remain the fast path,
     * while additional edge/camera/label shots can be supplied without changing the API contract.
     */
    suspend fun inspect(context: Context, captures: List<VisionCapture>): DeviceInspection {
        require(captures.size >= REQUIRED_PHOTOS) { "Take at least front and back photos before analysis." }
        require(captures.all { it.file.exists() }) { "One or more inspection photos are missing." }
        require(captures.map { it.angle.lowercase() }.toSet().size == captures.size) { "Each inspection angle must be unique." }
        val token = AuthManager.validAccessToken(context)
        return withContext(Dispatchers.IO) {
            val images = JSONArray()
            val order = JSONArray()
            captures.forEach { capture ->
                images.put(imageDataUrl(capture.file))
                order.put(MorleyVisionPolicy.clean(capture.angle).ifBlank { "additional" })
            }
            val response = edge(
                token,
                "device-inspection",
                JSONObject()
                    .put("image_data_urls", images)
                    .put("capture_order", order)
            )
            parseInspection(response)
        }
    }

    suspend fun livePricing(context: Context, inspection: DeviceInspection): LivePricingResult {
        MorleyVisionPolicy.pricingBlockReason(inspection)?.let { reason ->
            return LivePricingResult(
                listings = emptyList(),
                usedMedian = null,
                conditionAdjustedResale = null,
                recommendationBlockedReason = reason
            )
        }
        val query = inspection.identityQuery.trim()
        require(query.isNotBlank()) { "A verified device identity is required before live price research." }
        val token = AuthManager.validAccessToken(context)
        return withContext(Dispatchers.IO) {
            val response = edge(token, "market-search-v2", JSONObject().put("query", query).put("limit", 30))
            parsePricing(response, inspection)
        }
    }

    fun decodeDisplayBitmap(file: File, maxEdge: Int = 1800): Bitmap = decodeOriented(file, maxEdge)

    private fun imageDataUrl(file: File): String {
        var bitmap = decodeOriented(file, LONG_EDGE)
        try {
            var bytes = encodeJpeg(bitmap, intArrayOf(88, 82, 76, 70, 64, 58, 52))
            if (bytes.size > MAX_SINGLE_ENCODED_BYTES) {
                val longEdge = max(bitmap.width, bitmap.height)
                if (longEdge > FALLBACK_LONG_EDGE) {
                    val scale = FALLBACK_LONG_EDGE.toDouble() / longEdge.toDouble()
                    val smaller = Bitmap.createScaledBitmap(
                        bitmap,
                        max(1, (bitmap.width * scale).roundToInt()),
                        max(1, (bitmap.height * scale).roundToInt()),
                        true
                    )
                    if (smaller !== bitmap) {
                        bitmap.recycle()
                        bitmap = smaller
                    }
                    bytes = encodeJpeg(bitmap, intArrayOf(80, 72, 64, 56, 48, 40))
                }
            }
            require(bytes.size <= MAX_SINGLE_ENCODED_BYTES) {
                "This photo is too detailed to analyse safely. Retake it closer to the device."
            }
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeOriented(file: File, maxEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The captured photo could not be opened." }

        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > maxEdge * 2) sample *= 2
        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        ) ?: error("The captured photo could not be decoded.")

        var oriented = applyExif(decoded, file)
        if (oriented !== decoded) decoded.recycle()

        val longEdge = max(oriented.width, oriented.height)
        if (longEdge > maxEdge) {
            val scale = maxEdge.toDouble() / longEdge.toDouble()
            val scaled = Bitmap.createScaledBitmap(
                oriented,
                max(1, (oriented.width * scale).roundToInt()),
                max(1, (oriented.height * scale).roundToInt()),
                true
            )
            if (scaled !== oriented) oriented.recycle()
            oriented = scaled
        }
        return oriented
    }

    private fun applyExif(bitmap: Bitmap, file: File): Bitmap {
        val orientation = runCatching {
            ExifInterface(file).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun encodeJpeg(bitmap: Bitmap, qualities: IntArray): ByteArray {
        var best = ByteArray(0)
        for (quality in qualities) {
            val out = ByteArrayOutputStream()
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) { "Photo encoding failed." }
            best = out.toByteArray()
            if (best.size <= MAX_SINGLE_ENCODED_BYTES) break
        }
        return best
    }

    private fun edge(token: String, function: String, body: JSONObject): JSONObject {
        val connection = URL("${BuildConfig.SUPABASE_URL}/functions/v1/$function").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 45_000
            connection.doOutput = true
            connection.useCaches = false
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-store")
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching {
                    JSONObject(text).optString("error").ifBlank { JSONObject(text).optString("message") }
                }.getOrDefault("").ifBlank { "Request failed ($code)." }
                error(message)
            }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseInspection(root: JSONObject): DeviceInspection {
        val result = root.optJSONObject("result") ?: error("No device inspection result was returned.")
        val regions = result.optJSONArray("damage_regions").toDamageRegions()
            .filter { it.confidence >= MorleyVisionPolicy.MIN_DAMAGE_CONFIDENCE }
        return DeviceInspection(
            brand = MorleyVisionPolicy.clean(result.optString("likely_brand")),
            family = MorleyVisionPolicy.clean(result.optString("likely_family")),
            model = MorleyVisionPolicy.clean(result.optString("likely_model")),
            modelNumber = MorleyVisionPolicy.clean(result.optString("model_number")),
            colour = MorleyVisionPolicy.clean(result.optString("colour")),
            storage = MorleyVisionPolicy.clean(result.optString("storage")),
            conditionGrade = MorleyVisionPolicy.clean(result.optString("condition_grade")).ifBlank { "C" },
            conditionSummary = MorleyVisionPolicy.clean(result.optString("condition_summary")),
            confidence = result.optDouble("confidence", 0.0).coerceIn(0.0, 1.0),
            damageFlags = result.optJSONArray("damage_flags").toStrings(),
            damageRegions = regions,
            evidence = result.optJSONArray("evidence").toStrings(),
            uncertainties = result.optJSONArray("uncertainties").toStrings(),
            nextPhotos = result.optJSONArray("next_photos").toStrings(),
            catalogueMatch = root.optJSONObject("catalogue_match")?.let(::parseCatalogueMatch),
            qualityWarnings = result.optJSONArray("quality_warnings").toStrings(),
            consistencyWarnings = result.optJSONArray("consistency_warnings").toStrings(),
            componentFindings = result.optJSONArray("component_findings").toStrings()
        )
    }

    private fun parseCatalogueMatch(value: JSONObject): CatalogueMatch = CatalogueMatch(
        id = MorleyVisionPolicy.clean(value.optString("id")),
        category = MorleyVisionPolicy.clean(value.optString("category")),
        brand = MorleyVisionPolicy.clean(value.optString("brand")),
        family = MorleyVisionPolicy.clean(value.optString("family")),
        modelName = MorleyVisionPolicy.clean(value.optString("model_name")),
        modelNumber = MorleyVisionPolicy.clean(value.optString("model_number")),
        releaseYear = value.optInt("release_year").takeIf { it > 0 },
        storageOptions = value.optJSONArray("storage_options").toStrings(),
        imageReferenceUrl = MorleyVisionPolicy.clean(value.optString("image_reference_url")).takeIf { it.isNotBlank() }
    )

    private fun parsePricing(root: JSONObject, inspection: DeviceInspection): LivePricingResult {
        val allUsed = buildList {
            addAll(root.optJSONObject("ebay")?.optJSONArray("items").toMarketListings("eBay AU"))
            addAll(root.optJSONObject("facebook")?.optJSONArray("items").toMarketListings("Facebook Marketplace"))
        }.filter { it.price > 0.0 }
            .distinctBy { "${it.source.lowercase()}|${it.title.lowercase()}|${it.price}" }

        val identityMatched = allUsed.filter { MorleyVisionPolicy.titleMatchesIdentity(it.title, inspection) }
        val comparables = MorleyVisionPolicy.robustComparableSet(identityMatched)
        val rejected = allUsed.size - comparables.size

        if (comparables.size < MorleyVisionPolicy.MIN_COMPARABLES_FOR_PRICE) {
            return LivePricingResult(
                listings = comparables.sortedBy { it.price }.take(12),
                usedMedian = null,
                conditionAdjustedResale = null,
                rejectedListingCount = rejected,
                verifiedComparableCount = comparables.size,
                recommendationBlockedReason = "Not enough verified market data to suggest a price."
            )
        }

        val median = MorleyVisionPolicy.median(comparables.map { it.price })
        val adjustment = ConditionAdjustment.assess(
            ConditionEvidence(
                observedCondition = gradeToCondition(inspection.conditionGrade),
                majorFaultCount = inspection.damageRegions.count { it.severity.equals("major", true) },
                missingAccessoryCount = 0
            )
        )
        return LivePricingResult(
            listings = comparables.sortedBy { it.price }.take(12),
            usedMedian = median,
            conditionAdjustedResale = median.times(adjustment.multiplier),
            rejectedListingCount = rejected,
            verifiedComparableCount = comparables.size
        )
    }

    private fun gradeToCondition(grade: String): ObservedCondition = when (grade.uppercase()) {
        "A" -> ObservedCondition.EXCELLENT
        "B" -> ObservedCondition.GOOD
        "C" -> ObservedCondition.FAIR
        "D" -> ObservedCondition.POOR
        "PARTS" -> ObservedCondition.PARTS
        else -> ObservedCondition.FAIR
    }

    private fun JSONArray?.toStrings(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                MorleyVisionPolicy.clean(optString(i)).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun JSONArray?.toDamageRegions(): List<DamageRegion> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val item = optJSONObject(i) ?: continue
                val photoIndex = item.optInt("photo_index")
                if (photoIndex < 1) continue
                add(
                    DamageRegion(
                        photoIndex = photoIndex,
                        label = MorleyVisionPolicy.clean(item.optString("label")).ifBlank { "Visible damage" },
                        severity = MorleyVisionPolicy.clean(item.optString("severity")).ifBlank { "minor" },
                        confidence = item.optDouble("confidence", 0.0).coerceIn(0.0, 1.0),
                        x = item.optDouble("x", 0.0).toFloat().coerceIn(0f, 1f),
                        y = item.optDouble("y", 0.0).toFloat().coerceIn(0f, 1f),
                        width = item.optDouble("width", 0.0).toFloat().coerceIn(0.02f, 1f),
                        height = item.optDouble("height", 0.0).toFloat().coerceIn(0.02f, 1f)
                    )
                )
            }
        }
    }

    private fun JSONArray?.toMarketListings(fallbackSource: String): List<MarketListing> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val item = optJSONObject(i) ?: continue
                val price = item.optDouble("deliveredPrice", item.optDouble("price", 0.0))
                val title = MorleyVisionPolicy.clean(item.optString("title"))
                if (price <= 0.0 || title.isBlank()) continue
                add(
                    MarketListing(
                        source = MorleyVisionPolicy.clean(item.optString("source")).ifBlank { fallbackSource },
                        title = title,
                        price = price,
                        condition = MorleyVisionPolicy.clean(item.optString("condition"))
                    )
                )
            }
        }
    }
}
