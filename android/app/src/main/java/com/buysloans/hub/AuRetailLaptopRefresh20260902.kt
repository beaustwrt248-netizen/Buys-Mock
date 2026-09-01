package com.buysloans.hub

/**
 * Incremental Australian retail observations verified 2026-09-02.
 * This append-only refresh supplements AuRetailLaptopCatalog without rewriting older checked timestamps.
 * Prices are observational only and must not bypass exact-configuration matching or valuation/confidence protections.
 */
object AuRetailLaptopRefresh20260902 {
    const val CHECKED_AT = "2026-09-02T01:57:32+08:00"

    val listings = listOf(
        AuRetailLaptopListing(
            retailer = "Dell Australia",
            brand = "Dell",
            familyModel = "XPS 13 Laptop (2026)",
            modelSku = "DX13260 / cdx13260cto01mau",
            processor = "Intel Core 5 320",
            ram = "8GB",
            storage = "512GB",
            operatingSystem = "Windows 11 Home",
            priceAud = 1399.20,
            sourceUrl = "https://www.dell.com/en-au/shop/dell-laptops/xps-13-laptop-2026/spd/xps13dx13260laptop/cdx13260cto01mau",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Microsoft Australia",
            brand = "Microsoft",
            familyModel = "Surface Laptop 15-inch (8th Edition)",
            modelSku = "8MZBMMCJZP9W",
            processor = "Snapdragon X2 Plus 10 Core",
            ram = "16GB",
            storage = "512GB",
            operatingSystem = "Windows 11",
            priceAud = 2899.0,
            sourceUrl = "https://www.microsoft.com/en-au/store/configure/surface-laptop-15-inch-8th-edition/8mzbmmcjzp9w",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Apple Australia",
            brand = "Apple",
            familyModel = "MacBook Neo 13-inch (2026)",
            modelSku = null,
            processor = "Apple A18 Pro",
            ram = "8GB",
            storage = "256GB",
            operatingSystem = "macOS",
            priceAud = 899.0,
            sourceUrl = "https://www.apple.com/au/macbook-neo/specs/",
            checkedAtIso = CHECKED_AT
        )
    )

    val currentListings = listings.filter { it.status == RetailListingStatus.CURRENT }
}
