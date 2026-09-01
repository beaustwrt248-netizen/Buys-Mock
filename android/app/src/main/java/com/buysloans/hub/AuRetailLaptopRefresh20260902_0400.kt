package com.buysloans.hub

/**
 * Incremental Australian retail observations verified 2026-09-02 04:00 AWST.
 * Prices are observational only and must not bypass exact-configuration matching,
 * confidence, valuation or max-buy protections.
 */
object AuRetailLaptopRefresh20260902_0400 {
    const val CHECKED_AT = "2026-09-02T04:00:00+08:00"

    val listings = listOf(
        AuRetailLaptopListing(
            retailer = "JB Hi-Fi",
            brand = "HP",
            familyModel = "Chromebook x360 14",
            modelSku = "D46WXPA#ABG / 897357",
            processor = "Intel Processor N150",
            ram = "8GB",
            storage = "128GB",
            operatingSystem = "ChromeOS",
            priceAud = 899.0,
            sourceUrl = "https://www.jbhifi.com.au/products/hp-x360-14-wqxga-touch-chromebook-intel-n150128gb",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "ASUS Australia",
            brand = "ASUS",
            familyModel = "Chromebook CM14 (CM1405)",
            modelSku = "CM1405CM4A-S60064",
            processor = "MediaTek Kompanio 540",
            ram = "4GB",
            storage = "64GB",
            operatingSystem = "ChromeOS",
            priceAud = 599.0,
            sourceUrl = "https://www.asus.com/au/laptops/for-home/chromebook/asus-chromebook-cm14-cm1405/shop-asus-chromebook-cm14-cm1405cm4a/",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "ASUS Australia",
            brand = "ASUS",
            familyModel = "Chromebook CM14 (CM1405)",
            modelSku = "CM1405CM4A-S64128",
            processor = "MediaTek Kompanio 540",
            ram = "4GB",
            storage = "128GB",
            operatingSystem = "ChromeOS",
            priceAud = 699.0,
            sourceUrl = "https://www.asus.com/au/laptops/for-home/chromebook/asus-chromebook-cm14-cm1405/shop-asus-chromebook-cm14-cm1405cm4a/",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Acer Australia",
            brand = "Acer",
            familyModel = "Aspire 14",
            modelSku = "NX.KRWSA.001",
            processor = "Intel Core 5 120U",
            ram = "16GB",
            storage = "512GB",
            operatingSystem = "Windows 11 Home",
            priceAud = 1499.0,
            sourceUrl = "https://store.acer.com/en-au/laptops",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Acer Australia",
            brand = "Acer",
            familyModel = "Aspire 14 AI",
            modelSku = "NX.JL1SA.001",
            processor = "AMD Ryzen AI 5 340",
            ram = "16GB",
            storage = "512GB",
            operatingSystem = "Windows 11 Home",
            priceAud = 1599.0,
            sourceUrl = "https://store.acer.com/en-au/laptops",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Acer Australia",
            brand = "Acer",
            familyModel = "Swift Go 14 AI",
            modelSku = "NX.JNBSA.001",
            processor = "Intel Core Ultra 5 226V",
            ram = "16GB",
            storage = "512GB",
            operatingSystem = "Windows 11 Home",
            priceAud = 1799.0,
            sourceUrl = "https://store.acer.com/en-au/laptops",
            checkedAtIso = CHECKED_AT
        )
    )

    val currentListings = listings.filter { it.status == RetailListingStatus.CURRENT }
}
