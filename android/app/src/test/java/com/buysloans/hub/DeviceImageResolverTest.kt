package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceImageResolverTest {
    @Test fun directImagesAreAccepted() {
        assertEquals("https://example.com/device.webp", DeviceImageResolver.directImageUrl("https://example.com/device.webp"))
        assertEquals("https://example.com/device.png?v=2", DeviceImageResolver.directImageUrl("https://example.com/device.png?v=2"))
    }

    @Test fun productPagesAreNotDecodedAsImages() {
        assertNull(DeviceImageResolver.directImageUrl("https://www.samsung.com/au/smartphones/galaxy-s/"))
        assertTrue(DeviceImageResolver.hasImageReference("https://www.samsung.com/au/smartphones/galaxy-s/"))
    }

    @Test fun accessibilityLabelsUseDeviceIdentity() {
        assertEquals("Samsung Galaxy S26", DeviceImageResolver.accessibilityLabel("Samsung", "Galaxy S26"))
    }
}
