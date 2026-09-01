package com.buysloans.hub

/**
 * Incremental Australian retail observations verified 2026-09-02 02:56 AWST.
 * Prices are observational only and must not bypass exact-configuration matching,
 * confidence, valuation or max-buy protections.
 */
object AuRetailLaptopRefresh20260902_0256 {
    const val CHECKED_AT = "2026-09-02T02:56:49+08:00"

    val listings = listOf(
        AuRetailLaptopListing(
            retailer = "ASUS Australia",
            brand = "ASUS",
            familyModel = "Chromebook CX14 (CX1405)",
            modelSku = "CX1405CTA-S38128",
            processor = "Intel Core 3 N355",
            ram = "8GB",
            storage = "128GB",
            operatingSystem = "ChromeOS",
            priceAud = 489.0,
            sourceUrl = "https://www.asus.com/au/laptops/for-home/chromebook/asus-chromebook-cx14-cx1405/shop-asus-chromebook-cx14-cx1405cta/",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Microsoft Australia",
            brand = "Microsoft",
            familyModel = "Surface Laptop 15-inch (8th Edition)",
            modelSku = "8MZBMMCJZP9W",
            processor = "Snapdragon X2 Elite 12 Core",
            ram = "16GB",
            storage = "512GB",
            operatingSystem = "Windows 11",
            priceAud = 2999.0,
            sourceUrl = "https://www.microsoft.com/en-au/store/b/Surface",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Harvey Norman",
            brand = "HP",
            familyModel = "OmniBook Ultra 14",
            modelSku = "14-KD0008TU / E41K4PA",
            processor = "Intel Core Ultra 7 356H",
            ram = "16GB",
            storage = "512GB",
            operatingSystem = "Windows 11 Home",
            priceAud = 3699.0,
            sourceUrl = "https://www.harveynorman.com.au/hp-omnibook-ultra-14-inch-core-ultra-7-356h-16gb-512gb-ssd-next-gen-ai-laptop-silk-sand.html",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Officeworks",
            brand = "Lenovo",
            familyModel = "IdeaPad Slim 3 15.3",
            modelSku = "LE83K1001F",
            processor = "Intel Core i5",
            ram = "16GB",
            storage = "512GB",
            operatingSystem = "Windows 11",
            priceAud = 997.0,
            sourceUrl = "https://www.officeworks.com.au/shop/officeworks/p/lenovo-15-3-ideapad-slim-3-core-i5-16-512gb-laptop-le83k1001f",
            checkedAtIso = CHECKED_AT,
            status = RetailListingStatus.UNAVAILABLE
        )
    )

    val currentListings = listings.filter { it.status == RetailListingStatus.CURRENT }
}
