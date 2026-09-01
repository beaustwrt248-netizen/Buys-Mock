package com.buysloans.hub

/**
 * Incremental Australian retail observations verified 2026-09-02 04:54 AWST.
 * Prices are observational only and must not bypass exact-configuration matching,
 * confidence, valuation or max-buy protections. Prior snapshots remain historical.
 */
object AuRetailLaptopRefresh20260902_0454 {
    const val CHECKED_AT = "2026-09-02T04:54:40+08:00"

    val listings = listOf(
        AuRetailLaptopListing(
            retailer = "The Good Guys",
            brand = "Lenovo",
            familyModel = "IdeaPad Slim 3 14-inch Chromebook",
            modelSku = "83SX000MAU",
            processor = "MediaTek Kompanio 540",
            ram = "8GB",
            storage = "128GB",
            operatingSystem = "ChromeOS",
            priceAud = 649.0,
            sourceUrl = "https://www.thegoodguys.com.au/lenovo-ideapad-slim-3-14-inches-mtk540-8gb-128gb-touch-chromebook-83sx000mau",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "The Good Guys",
            brand = "Lenovo",
            familyModel = "IdeaPad 3 15.6-inch Chromebook",
            modelSku = "82N4005GAU",
            processor = "Intel Celeron N4500",
            ram = "8GB",
            storage = "128GB",
            operatingSystem = "ChromeOS",
            priceAud = 444.0,
            sourceUrl = "https://www.thegoodguys.com.au/lenovo-ideapad-3-156-inches-celeron-n4500-8gb-128gb-chromebook-82n4005gau",
            checkedAtIso = CHECKED_AT,
            status = RetailListingStatus.CLEARANCE
        ),
        AuRetailLaptopListing(
            retailer = "The Good Guys",
            brand = "Lenovo",
            familyModel = "IdeaPad Flex 3i 12.2-inch Chromebook",
            modelSku = "82XH001XAU",
            processor = "Intel Processor N100",
            ram = "4GB",
            storage = "128GB",
            operatingSystem = "ChromeOS",
            priceAud = 629.0,
            sourceUrl = "https://www.thegoodguys.com.au/lenovo-ideapad-flex-3i-122-inches-intel-n100-4gb-128gb-emmc-chromebook-82xh001xau",
            checkedAtIso = CHECKED_AT,
            status = RetailListingStatus.CLEARANCE
        ),
        AuRetailLaptopListing(
            retailer = "The Good Guys",
            brand = "ASUS",
            familyModel = "Chromebook CX14",
            modelSku = "CX1405CTA-S58128",
            processor = "Intel Processor N50",
            ram = "8GB",
            storage = "128GB",
            operatingSystem = "ChromeOS",
            priceAud = 494.0,
            sourceUrl = "https://www.thegoodguys.com.au/asus-14-inches-n50-8gb-128gb-chromebook-cx1405cta-s58128",
            checkedAtIso = CHECKED_AT,
            status = RetailListingStatus.CLEARANCE
        ),
        AuRetailLaptopListing(
            retailer = "Acer Australia",
            brand = "Acer",
            familyModel = "Aspire 16 AI",
            modelSku = "NX.JLLSA.002",
            processor = "AMD Ryzen AI 5 340",
            ram = "16GB",
            storage = "1TB",
            operatingSystem = "Windows 11 Home",
            priceAud = 1899.0,
            sourceUrl = "https://store.acer.com/en-au/acer-aspire-16ai-windows-11-home-amd-ryzen-16gb-ram-1024gb-ssd",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Acer Australia",
            brand = "Acer",
            familyModel = "Swift Go 16 AI",
            modelSku = "NX.JNMSA.004",
            processor = "Intel Core Ultra 7 258V",
            ram = "16GB",
            storage = "512GB",
            operatingSystem = "Windows 11 Home",
            priceAud = 2499.0,
            sourceUrl = "https://store.acer.com/en-au/swift-go-16-ai-windows-11-home-intel-core-uitra7-16gb-ram-512gb-ssd-oled-slimbezel-1",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Acer Australia",
            brand = "Acer",
            familyModel = "Nitro V 16",
            modelSku = "NH.U59SA.005",
            processor = "Intel Core 7 240H",
            ram = "16GB",
            storage = "1TB",
            operatingSystem = "Windows 11 Home",
            priceAud = 2899.0,
            sourceUrl = "https://store.acer.com/en-au/laptops",
            checkedAtIso = CHECKED_AT
        )
    )

    val currentListings = listings.filter { it.status == RetailListingStatus.CURRENT }
}
