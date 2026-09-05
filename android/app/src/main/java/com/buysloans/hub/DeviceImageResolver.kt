package com.buysloans.hub

/**
 * Canonical image selection for live device-catalogue records.
 *
 * imageReferenceUrl is preferred when it is a direct image. Historical catalogue
 * rows may contain a manufacturer product/support page instead; those are kept as
 * references but must not be passed to an image decoder as if they were JPEG/PNG.
 */
object DeviceImageResolver {
    private val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".avif")

    fun directImageUrl(imageReferenceUrl: String?): String? {
        val value = imageReferenceUrl?.trim().orEmpty()
        if (value.isBlank()) return null
        val lower = value.substringBefore('#').substringBefore('?').lowercase()
        return value.takeIf { imageExtensions.any(lower::endsWith) }
    }

    fun hasImageReference(imageReferenceUrl: String?): Boolean = !imageReferenceUrl.isNullOrBlank()

    fun accessibilityLabel(brand: String?, modelName: String?): String =
        listOfNotNull(brand?.trim()?.takeIf(String::isNotBlank), modelName?.trim()?.takeIf(String::isNotBlank))
            .joinToString(" ")
            .ifBlank { "Device image" }
}
