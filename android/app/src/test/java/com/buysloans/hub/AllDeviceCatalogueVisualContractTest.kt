package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AllDeviceCatalogueVisualContractTest {
    private val source = File("src/main/java/com/buysloans/hub/LiveDeviceCatalogueBrowser.kt").readText()
    private val loader = File("src/main/java/com/buysloans/hub/DeviceCataloguePhoto.kt").readText()

    @Test
    fun allDeviceCatalogueUsesDeviceAwareVisuals() {
        assertTrue(source.contains("CatalogueDevicePhoto(device)"))
        assertTrue(source.contains("MobilePhonePhoto("))
        assertTrue(source.contains("PricingCategoryVisual("))
    }

    @Test
    fun allDeviceCataloguePassesExactIdentityIntoImageVerification() {
        assertTrue(source.contains("imageReferenceUrl = device.imageReferenceUrl"))
        assertTrue(source.contains("modelNumber = device.modelNumber"))
        assertTrue(source.contains("allowLiveReference = false"))
        assertTrue(loader.contains("cataloguePageTitleMatchesDevice"))
        assertTrue(loader.contains("catalogueNearbyImageForExactModel"))
        assertTrue(loader.contains("resolveVerifiedProductImage"))
    }

    @Test
    fun sharedSourcePagesCannotReuseAnotherModelsCachedImage() {
        assertTrue(loader.contains("normalizeCatalogueIdentity(model)"))
        assertTrue(loader.contains("normalizeCatalogueIdentity(modelNumber.orEmpty())"))
        assertTrue(loader.contains("listOf(referenceUrl"))
    }

    @Test
    fun genericPageBodyCannotAuthorizeWrongDeviceArtwork() {
        assertFalse(loader.contains("if (html.contains(model"))
        assertTrue(loader.contains("<title[^>]*>"))
        assertTrue(loader.contains("og:title"))
    }

    @Test
    fun nonPhoneCategoriesKeepCategoryAwareFallbacks() {
        assertTrue(source.contains("PricingVisual.LAPTOP"))
        assertTrue(source.contains("PricingVisual.DESKTOP"))
        assertTrue(source.contains("PricingVisual.CONSOLE"))
        assertTrue(source.contains("\"tablet\", \"wearable\" -> PricingVisual.PHONE"))
    }

    @Test
    fun allDeviceCatalogueDoesNotFallBackToBrandInitials() {
        assertFalse(source.contains("device.brand.take(2).uppercase()"))
    }
}
