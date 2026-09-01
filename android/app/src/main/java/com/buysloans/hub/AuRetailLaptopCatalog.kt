package com.buysloans.hub

enum class RetailListingStatus { CURRENT, CLEARANCE, UNAVAILABLE }

data class AuRetailLaptopListing(
    val retailer: String,
    val brand: String,
    val familyModel: String,
    val modelSku: String?,
    val processor: String,
    val ram: String,
    val storage: String,
    val operatingSystem: String,
    val priceAud: Double,
    val sourceUrl: String,
    val checkedAtIso: String,
    val status: RetailListingStatus = RetailListingStatus.CURRENT
)

/**
 * Point-in-time Australian retail observations used only to improve guided identity support.
 * These prices must never bypass exact-configuration matching, confidence or valuation protections.
 */
object AuRetailLaptopCatalog {
    const val CHECKED_AT = "2026-09-02T05:53:40+08:00"

    val listings = listOf(
        AuRetailLaptopListing(
            retailer = "JB Hi-Fi", brand = "Lenovo", familyModel = "Chromebook Plus 14 OLED",
            modelSku = "83MY000PAU / 816938", processor = "MediaTek Kompanio Ultra 910",
            ram = "16GB", storage = "256GB", operatingSystem = "ChromeOS", priceAud = 1399.0,
            sourceUrl = "https://www.jbhifi.com.au/products/lenovo-14-oled-touchscreen-chromebook-plus-laptop-with-gemini-256gb",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "JB Hi-Fi", brand = "ASUS", familyModel = "Chromebook CX14",
            modelSku = "CX1405CTA-S58128 / 807672", processor = "Intel Processor N50",
            ram = "8GB", storage = "128GB", operatingSystem = "ChromeOS", priceAud = 599.0,
            sourceUrl = "https://www.jbhifi.com.au/products/asus-cx14-14-full-hd-chromebook-intel-n50-8gb-128gb",
            checkedAtIso = CHECKED_AT, status = RetailListingStatus.UNAVAILABLE
        ),
        AuRetailLaptopListing(
            retailer = "Officeworks", brand = "ASUS", familyModel = "Chromebook CX14",
            modelSku = "ASTA538128", processor = "Intel Core i3", ram = "8GB", storage = "128GB",
            operatingSystem = "ChromeOS", priceAud = 697.0,
            sourceUrl = "https://www.officeworks.com.au/shop/officeworks/p/asus-14-cx14-intel-core-i3-8-128gb-chromebook-asta538128",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Officeworks", brand = "Dell", familyModel = "Dell Pro 14",
            modelSku = "DEPB14250", processor = "Intel Core Ultra 7", ram = "32GB", storage = "512GB",
            operatingSystem = "Windows 11", priceAud = 3447.0,
            sourceUrl = "https://www.officeworks.com.au/shop/officeworks/p/dell-pro-14-laptop-ultra-7-32-512gb-depb14250",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Officeworks", brand = "HP", familyModel = "OmniBook 3 16",
            modelSku = "HPD46X7PA", processor = "Intel Core Ultra 5", ram = "16GB", storage = "512GB",
            operatingSystem = "Windows 11", priceAud = 1797.0,
            sourceUrl = "https://www.officeworks.com.au/shop/officeworks/p/hp-omnibook-3-16-copilot-pc-ultra-5-16-512gb-hpd46x7pa",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Officeworks", brand = "HP", familyModel = "OmniBook 5 14 OLED",
            modelSku = "HPHE0013QU", processor = "Snapdragon X", ram = "16GB", storage = "512GB",
            operatingSystem = "Windows 11", priceAud = 1047.0,
            sourceUrl = "https://www.officeworks.com.au/shop/officeworks/p/hp-14-omnibook-5-snapdragon-x-16-512gb-copilot-pc-hphe0013qu",
            checkedAtIso = CHECKED_AT, status = RetailListingStatus.CLEARANCE
        ),
        AuRetailLaptopListing(
            retailer = "The Good Guys", brand = "Lenovo", familyModel = "IdeaPad Slim 3 15.6",
            modelSku = "82XQ00SQAU", processor = "AMD Ryzen 5 7520U", ram = "16GB", storage = "512GB",
            operatingSystem = "Windows 11 Home", priceAud = 899.0,
            sourceUrl = "https://www.thegoodguys.com.au/lenovo-ideapad-slim-3-156-inches-ryzen-5-16gb-512gb-laptop-82xq00sqau",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "The Good Guys", brand = "ASUS", familyModel = "Vivobook 15",
            modelSku = "F1504VA-BQ015W", processor = "Intel Core 5", ram = "16GB", storage = "1TB",
            operatingSystem = "Windows 11 Home", priceAud = 999.0,
            sourceUrl = "https://www.thegoodguys.com.au/asus-vivobook-15-156-inches-5-core-16gb-1tb-laptop-f1504va-bq015w",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Harvey Norman", brand = "Lenovo", familyModel = "Yoga Slim 7i Aura 15.3",
            modelSku = "83HM000BAU", processor = "Intel Core Ultra 7 258V", ram = "32GB", storage = "1TB",
            operatingSystem = "Windows 11", priceAud = 2799.0,
            sourceUrl = "https://www.harveynorman.com.au/lenovo-yoga-slim-7i-aura-15-3-inch-intel-core-ultra-7-258v-32gb-1tb-ssd-next-gen-ai-copilot-pc-luna-grey.html",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Microsoft Australia", brand = "Microsoft", familyModel = "Surface Laptop 13-inch (1st Edition)",
            modelSku = null, processor = "Snapdragon X Plus 8 Core", ram = "16GB", storage = "256GB",
            operatingSystem = "Windows 11 Home", priceAud = 1647.0,
            sourceUrl = "https://www.microsoft.com/en-au/store/configure/surface-laptop-13-inch/8mzbmmcjzqv3",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Microsoft Australia", brand = "Microsoft", familyModel = "Surface Laptop 13.8-inch (8th Edition)",
            modelSku = null, processor = "Snapdragon X2 Plus 10 Core", ram = "16GB", storage = "512GB",
            operatingSystem = "Windows 11 Home", priceAud = 2799.0,
            sourceUrl = "https://www.microsoft.com/en-au/store/b/Surface",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Dell Australia", brand = "Dell", familyModel = "Dell 16 Plus",
            modelSku = "DB16250", processor = "Intel Core Ultra", ram = "Configurable", storage = "Configurable",
            operatingSystem = "Windows 11 Home", priceAud = 3498.0,
            sourceUrl = "https://www.dell.com/en-au/shop/dell-laptops/scr/laptops",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Dell Australia", brand = "Dell", familyModel = "Dell 16 Plus 2-in-1",
            modelSku = "DB06250", processor = "Intel Core Ultra", ram = "Configurable", storage = "Configurable",
            operatingSystem = "Windows 11 Home", priceAud = 1998.70,
            sourceUrl = "https://www.dell.com/en-au/shop/dell-laptops/scr/laptops",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Apple Australia", brand = "Apple", familyModel = "MacBook Air 13-inch (2026, M5)",
            modelSku = null, processor = "Apple M5", ram = "16GB", storage = "512GB",
            operatingSystem = "macOS", priceAud = 1799.0,
            sourceUrl = "https://www.apple.com/au/newsroom/2026/03/apple-introduces-the-new-macbook-air-with-m5/",
            checkedAtIso = CHECKED_AT
        ),
        AuRetailLaptopListing(
            retailer = "Apple Australia", brand = "Apple", familyModel = "MacBook Pro 14-inch (2026)",
            modelSku = null, processor = "Apple M5", ram = "16GB", storage = "1TB",
            operatingSystem = "macOS", priceAud = 3199.0,
            sourceUrl = "https://www.apple.com/au/shop/buy-mac/macbook-pro/14-inch-silver-standard-display-apple-m5-chip-10-core-cpu-10-core-gpu-16gb-memory-1tb-storage",
            checkedAtIso = CHECKED_AT
        )
    )

    val currentListings: List<AuRetailLaptopListing> = listings.filter { it.status == RetailListingStatus.CURRENT }
}
