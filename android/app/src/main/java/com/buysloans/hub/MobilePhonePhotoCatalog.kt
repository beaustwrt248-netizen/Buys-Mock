package com.buysloans.hub

/**
 * Official manufacturer product pages used to resolve real-device imagery at runtime.
 *
 * The UI reads each page's OpenGraph/Twitter product image asynchronously. This avoids
 * generated or illustrative replacement artwork and lets manufacturers remain the image source.
 * Exact-model pages are only used when the model matches; category pages are only used on the
 * brand/category chooser so an unrelated representative phone is never shown as an exact model.
 */
object MobilePhonePhotoCatalog {
    private val brandRepresentativePages = mapOf(
        "Apple" to "https://www.apple.com/au/iphone-17-pro/",
        "Samsung" to "https://www.samsung.com/au/smartphones/galaxy-s25-ultra/",
        "Google" to "https://store.google.com/au/product/pixel_10_pro",
        "OnePlus" to "https://www.oneplus.com/13",
        "Xiaomi" to "https://www.mi.com/global/product/xiaomi-15-ultra/",
        "OPPO" to "https://www.oppo.com/au/smartphones/series-find-x/find-x8-pro/",
        "Nothing" to "https://nothing.tech/pages/phone-3",
        "Motorola" to "https://www.motorola.com.au/smartphones-razr-60-ultra/p",
        "Vivo" to "https://www.vivo.com/en/products/x200-pro",
        "Realme" to "https://www.realme.com/global/realme-gt-7-pro",
        "Huawei" to "https://consumer.huawei.com/en/phones/pura70-ultra/",
        "HONOR" to "https://www.honor.com/au/phones/honor-magic7-pro/",
        "ASUS" to "https://rog.asus.com/phones/rog-phone-9-pro/",
        "HMD" to "https://www.hmd.com/en_au/hmd-skyline",
        "Nokia" to "https://www.hmd.com/en_au/nokia-g-42",
        "Fairphone" to "https://shop.fairphone.com/fairphone-5",
        "Meizu" to "https://www.meizu.com/en/21pro",
        "Lenovo" to "https://www.lenovo.com/au/en/c/phones/",
        "Sony" to "https://www.sony.com.au/smartphones",
        "ZTE / nubia" to "https://intl.nubia.com/products/nubia-z70-ultra",
        "TCL" to "https://www.tcl.com/global/en/mobile"
    )

    private val exactModelPages = mapOf(
        "Apple|iPhone 17 Pro Max" to "https://www.apple.com/au/iphone-17-pro/",
        "Apple|iPhone 17 Pro" to "https://www.apple.com/au/iphone-17-pro/",
        "Apple|iPhone 17" to "https://www.apple.com/au/iphone-17/",
        "Apple|iPhone Air" to "https://www.apple.com/au/iphone-air/",
        "Apple|iPhone 16 Pro Max" to "https://www.apple.com/au/iphone-16-pro/",
        "Apple|iPhone 16 Pro" to "https://www.apple.com/au/iphone-16-pro/",
        "Samsung|Galaxy S25 Ultra" to "https://www.samsung.com/au/smartphones/galaxy-s25-ultra/",
        "Samsung|Galaxy S25" to "https://www.samsung.com/au/smartphones/galaxy-s25/",
        "Samsung|Galaxy Z Fold 7" to "https://www.samsung.com/au/smartphones/galaxy-z-fold7/",
        "Samsung|Galaxy Z Flip 7" to "https://www.samsung.com/au/smartphones/galaxy-z-flip7/",
        "HONOR|Magic7 Pro" to "https://www.honor.com/au/phones/honor-magic7-pro/",
        "ASUS|ROG Phone 9 Pro" to "https://rog.asus.com/phones/rog-phone-9-pro/",
        "HMD|Skyline" to "https://www.hmd.com/en_au/hmd-skyline",
        "Fairphone|Fairphone 5" to "https://shop.fairphone.com/fairphone-5"
    )

    fun categoryPage(brand: String): String? = brandRepresentativePages[brand]
    fun exactModelPage(brand: String, model: String): String? = exactModelPages["$brand|$model"]
}
