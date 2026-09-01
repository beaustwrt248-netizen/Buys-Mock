package com.buysloans.hub

/**
 * ChromeOS device families shown in the guided laptop picker.
 *
 * These presets deliberately keep the same exact-configuration evidence rules as
 * the rest of the laptop flow. Adding a family only makes it selectable; it does
 * not relax matching, confidence, valuation or max-buy protections.
 */
object ChromebookSelectionCatalog {
    private val chromeRam = listOf("4GB", "8GB", "16GB")
    private val chromeStorage = listOf("32GB", "64GB", "128GB", "256GB", "512GB")

    private data class ChromeFamily(
        val brand: String,
        val name: String,
        val from: Int,
        val to: Int,
        val processors: List<String>
    )

    private val intelEntry = listOf(
        "Intel Celeron N4020",
        "Intel Celeron N4500",
        "Intel Processor N50",
        "Intel Processor N100",
        "Intel Processor N150",
        "Intel Processor N200",
        "Intel Pentium Silver N6000",
        "Intel Core i3",
        "Intel Core i5"
    )

    private val chromeArm = listOf(
        "MediaTek Kompanio 500",
        "MediaTek Kompanio 520",
        "MediaTek Kompanio 528",
        "MediaTek Kompanio 828",
        "MediaTek Kompanio Ultra 910",
        "Qualcomm Snapdragon 7c Gen 2"
    )

    private val families = listOf(
        ChromeFamily("Google", "Pixelbook", 2017, 2019, listOf("Intel Core i5", "Intel Core i7")),
        ChromeFamily("Google", "Pixelbook Go", 2019, 2022, listOf("Intel Core m3", "Intel Core i5", "Intel Core i7")),

        ChromeFamily("Acer", "Chromebook 311", 2019, 2026, intelEntry + chromeArm),
        ChromeFamily("Acer", "Chromebook 314", 2019, 2026, intelEntry),
        ChromeFamily("Acer", "Chromebook 315", 2019, 2026, intelEntry),
        ChromeFamily("Acer", "Chromebook Spin 311", 2019, 2025, intelEntry + chromeArm),
        ChromeFamily("Acer", "Chromebook Spin 314", 2021, 2026, intelEntry),
        ChromeFamily("Acer", "Chromebook Spin 513", 2021, 2024, chromeArm),
        ChromeFamily("Acer", "Chromebook Plus 514", 2023, 2026, listOf("Intel Core i3", "Intel Core i5", "AMD Ryzen 3", "AMD Ryzen 5")),
        ChromeFamily("Acer", "Chromebook Plus 515", 2023, 2026, listOf("Intel Core i3", "Intel Core i5")),
        ChromeFamily("Acer", "Chromebook Plus Spin 714", 2023, 2026, listOf("Intel Core i3", "Intel Core i5", "Intel Core Ultra 5")),

        ChromeFamily("ASUS", "Chromebook CX1", 2021, 2026, intelEntry),
        ChromeFamily("ASUS", "Chromebook CX14", 2025, 2026, listOf("Intel Processor N50", "Intel Core i3", "Intel Core 3 N355")),
        ChromeFamily("ASUS", "Chromebook CX34", 2023, 2026, listOf("Intel Core i3", "Intel Core i5")),
        ChromeFamily("ASUS", "Chromebook Plus CX34", 2023, 2026, listOf("Intel Core i3", "Intel Core i5")),
        ChromeFamily("ASUS", "Chromebook CM14", 2025, 2026, listOf("MediaTek Kompanio 540")),
        ChromeFamily("ASUS", "Chromebook Flip CM3", 2021, 2025, chromeArm),
        ChromeFamily("ASUS", "Chromebook Flip CM5", 2021, 2025, listOf("AMD Ryzen 3", "AMD Ryzen 5")),
        ChromeFamily("ASUS", "Chromebook Flip CX5", 2021, 2026, listOf("Intel Core i3", "Intel Core i5", "Intel Core i7")),

        ChromeFamily("Lenovo", "Chromebook Duet", 2020, 2026, chromeArm),
        ChromeFamily("Lenovo", "IdeaPad Flex 3 Chromebook", 2020, 2026, intelEntry + chromeArm),
        ChromeFamily("Lenovo", "IdeaPad Flex 5 Chromebook", 2020, 2026, listOf("Intel Core i3", "Intel Core i5")),
        ChromeFamily("Lenovo", "IdeaPad Slim 3 Chromebook", 2023, 2026, intelEntry + chromeArm),
        ChromeFamily("Lenovo", "Chromebook Plus 5i", 2023, 2026, listOf("Intel Core i3", "Intel Core i5")),
        ChromeFamily("Lenovo", "Chromebook Plus 14 OLED", 2026, 2026, listOf("MediaTek Kompanio Ultra 910")),

        ChromeFamily("HP", "Chromebook 11", 2013, 2024, intelEntry + chromeArm),
        ChromeFamily("HP", "Chromebook 14", 2013, 2026, intelEntry + chromeArm),
        ChromeFamily("HP", "Chromebook x360 14", 2018, 2026, listOf("Intel Celeron N4500", "Intel Processor N150", "Intel Pentium Silver N6000", "Intel Core i3", "Intel Core i5")),
        ChromeFamily("HP", "Chromebook Plus x360 14", 2023, 2026, listOf("Intel Core i3", "Intel Core i5")),

        ChromeFamily("Dell", "Chromebook 3100", 2019, 2024, listOf("Intel Celeron N4020")),
        ChromeFamily("Dell", "Chromebook 3110", 2022, 2026, listOf("Intel Celeron N4500", "Intel Processor N100", "Intel Processor N200")),
        ChromeFamily("Dell", "Latitude Chromebook Enterprise", 2019, 2026, listOf("Intel Core i3", "Intel Core i5", "Intel Core i7")),

        ChromeFamily("Samsung", "Chromebook 4", 2019, 2024, listOf("Intel Celeron N4000", "Intel Celeron N4020")),
        ChromeFamily("Samsung", "Chromebook 4+", 2019, 2024, listOf("Intel Celeron N4000", "Intel Celeron N4020")),
        ChromeFamily("Samsung", "Galaxy Chromebook", 2020, 2022, listOf("Intel Core i5")),
        ChromeFamily("Samsung", "Galaxy Chromebook 2", 2021, 2024, listOf("Intel Celeron 5205U", "Intel Core i3")),
        ChromeFamily("Samsung", "Galaxy Chromebook Go", 2021, 2026, listOf("Intel Celeron N4500")),
        ChromeFamily("Samsung", "Galaxy Chromebook Plus", 2024, 2026, listOf("Intel Core 3", "Intel Core 5"))
    )

    val presets: List<LaptopPreset> = families.flatMap { family ->
        (family.from..family.to).map { year ->
            LaptopPreset(
                brand = family.brand,
                model = "${family.name} (${year})",
                year = year,
                processors = family.processors.distinct(),
                ramOptions = chromeRam,
                storageOptions = chromeStorage
            )
        }
    }
}
