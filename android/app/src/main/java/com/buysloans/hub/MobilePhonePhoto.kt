package com.buysloans.hub

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MobilePhonePhoto(
    brand: String,
    model: String? = null,
    imageReferenceUrl: String? = null,
    modifier: Modifier = Modifier,
    categoryRepresentative: Boolean = false,
) {
    val liveReference = model?.let { LiveDevicePricing.device("mobile_phone", brand, it)?.imageReferenceUrl }
    val referenceUrl = remember(brand, model, imageReferenceUrl, liveReference, categoryRepresentative) {
        imageReferenceUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: liveReference?.trim()?.takeIf { it.isNotBlank() }
            ?: when {
                model != null -> MobilePhonePhotoCatalog.exactModelPage(brand, model)
                categoryRepresentative -> MobilePhonePhotoCatalog.categoryPage(brand)
                else -> null
            }
    }

    DeviceCataloguePhoto(
        brand = brand,
        model = model ?: "phone",
        imageReferenceUrl = referenceUrl,
        modifier = modifier,
    ) {
        PhoneBrandVisual(
            when (brand) {
                "Apple", "Samsung", "Google", "OnePlus", "Xiaomi" -> brand
                else -> "Other Brands"
            },
            Modifier.size(34.dp),
        )
    }
}
