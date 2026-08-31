package com.buysloans.hub

data class LaptopPreset(
    val brand: String,
    val model: String,
    val year: Int,
    val processors: List<String>,
    val ramOptions: List<String>,
    val storageOptions: List<String>
)

object LaptopSelectionCatalog {
    private val modernRam = listOf("4GB", "8GB", "16GB", "32GB", "64GB")
    private val modernStorage = listOf("128GB", "256GB", "512GB", "1TB", "2TB", "4TB")

    private data class Family(
        val brand: String,
        val name: String,
        val from: Int,
        val to: Int,
        val processors: List<String>
    )

    private val families = listOf(
        Family("Dell", "XPS 13", 2012, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7", "Snapdragon X Elite")),
        Family("Dell", "Latitude", 2006, 2026, listOf("Intel Core 2 Duo", "Intel Core i3", "Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7")),
        Family("Dell", "Inspiron", 2006, 2026, listOf("Intel Core 2 Duo", "Intel Core i3", "Intel Core i5", "Intel Core i7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("Lenovo", "ThinkPad", 2006, 2026, listOf("Intel Core 2 Duo", "Intel Core i3", "Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("Lenovo", "IdeaPad", 2008, 2026, listOf("Intel Core i3", "Intel Core i5", "Intel Core i7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("Lenovo", "Yoga", 2012, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("Lenovo", "Legion", 2017, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core i9", "AMD Ryzen 5", "AMD Ryzen 7", "AMD Ryzen 9")),
        Family("HP", "EliteBook", 2008, 2026, listOf("Intel Core 2 Duo", "Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("HP", "ProBook", 2009, 2026, listOf("Intel Core i3", "Intel Core i5", "Intel Core i7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("HP", "Pavilion", 2006, 2026, listOf("Intel Core 2 Duo", "Intel Core i3", "Intel Core i5", "Intel Core i7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("HP", "Spectre x360", 2015, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7")),
        Family("HP", "Envy", 2009, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("ASUS", "ZenBook", 2011, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("ASUS", "VivoBook", 2011, 2026, listOf("Intel Core i3", "Intel Core i5", "Intel Core i7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("ASUS", "ROG Zephyrus", 2017, 2026, listOf("Intel Core i7", "Intel Core i9", "AMD Ryzen 7", "AMD Ryzen 9")),
        Family("ASUS", "TUF Gaming", 2017, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core i9", "AMD Ryzen 5", "AMD Ryzen 7", "AMD Ryzen 9")),
        Family("Acer", "Aspire", 2006, 2026, listOf("Intel Core 2 Duo", "Intel Core i3", "Intel Core i5", "Intel Core i7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("Acer", "Swift", 2016, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("Acer", "Nitro", 2015, 2026, listOf("Intel Core i5", "Intel Core i7", "AMD Ryzen 5", "AMD Ryzen 7")),
        Family("Microsoft", "Surface Laptop", 2017, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7", "Snapdragon X Plus", "Snapdragon X Elite")),
        Family("Microsoft", "Surface Book", 2015, 2020, listOf("Intel Core i5", "Intel Core i7")),
        Family("MSI", "Stealth", 2013, 2026, listOf("Intel Core i7", "Intel Core i9", "Intel Core Ultra 7", "Intel Core Ultra 9")),
        Family("MSI", "Raider", 2016, 2026, listOf("Intel Core i7", "Intel Core i9", "Intel Core Ultra 9")),
        Family("Razer", "Blade", 2012, 2026, listOf("Intel Core i7", "Intel Core i9", "AMD Ryzen 9")),
        Family("Samsung", "Galaxy Book", 2021, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7", "Snapdragon X Elite")),
        Family("LG", "Gram", 2015, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7")),
        Family("Toshiba", "Satellite", 2006, 2020, listOf("Intel Core 2 Duo", "Intel Core i3", "Intel Core i5", "Intel Core i7")),
        Family("Dynabook", "Tecra", 2019, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7")),
        Family("Fujitsu", "LifeBook", 2006, 2026, listOf("Intel Core 2 Duo", "Intel Core i3", "Intel Core i5", "Intel Core i7")),
        Family("Panasonic", "Toughbook", 2006, 2026, listOf("Intel Core 2 Duo", "Intel Core i5", "Intel Core i7")),
        Family("Framework", "Laptop 13", 2021, 2026, listOf("Intel Core i5", "Intel Core i7", "Intel Core Ultra 5", "Intel Core Ultra 7", "AMD Ryzen 5", "AMD Ryzen 7"))
    )

    private val apple = listOf(
        LaptopPreset("Apple", "MacBook Pro 15-inch (2006)", 2006, listOf("Intel Core Duo", "Intel Core 2 Duo"), listOf("1GB", "2GB", "4GB"), listOf("80GB", "100GB", "120GB")),
        LaptopPreset("Apple", "MacBook Air 13-inch (2008)", 2008, listOf("Intel Core 2 Duo"), listOf("2GB"), listOf("80GB", "128GB")),
        LaptopPreset("Apple", "MacBook Pro 13-inch (2009)", 2009, listOf("Intel Core 2 Duo"), listOf("2GB", "4GB", "8GB"), listOf("160GB", "250GB", "320GB", "500GB")),
        LaptopPreset("Apple", "MacBook Pro 13-inch (2011)", 2011, listOf("Intel Core i5", "Intel Core i7"), listOf("4GB", "8GB", "16GB"), listOf("320GB", "500GB", "750GB")),
        LaptopPreset("Apple", "MacBook Pro Retina 13-inch (2012)", 2012, listOf("Intel Core i5", "Intel Core i7"), listOf("8GB", "16GB"), listOf("128GB", "256GB", "512GB", "768GB")),
        LaptopPreset("Apple", "MacBook Pro Retina 13-inch (2013)", 2013, listOf("Intel Core i5", "Intel Core i7"), listOf("4GB", "8GB", "16GB"), listOf("128GB", "256GB", "512GB", "1TB")),
        LaptopPreset("Apple", "MacBook Pro Retina 13-inch (2014)", 2014, listOf("Intel Core i5", "Intel Core i7"), listOf("8GB", "16GB"), listOf("128GB", "256GB", "512GB", "1TB")),
        LaptopPreset("Apple", "MacBook Pro Retina 13-inch (2015)", 2015, listOf("Intel Core i5", "Intel Core i7"), listOf("8GB", "16GB"), listOf("128GB", "256GB", "512GB", "1TB")),
        LaptopPreset("Apple", "MacBook Pro 13-inch (2016)", 2016, listOf("Intel Core i5", "Intel Core i7"), listOf("8GB", "16GB"), listOf("256GB", "512GB", "1TB")),
        LaptopPreset("Apple", "MacBook Pro 13-inch (2017)", 2017, listOf("Intel Core i5", "Intel Core i7"), listOf("8GB", "16GB"), listOf("128GB", "256GB", "512GB", "1TB")),
        LaptopPreset("Apple", "MacBook Air 13-inch (2018)", 2018, listOf("Intel Core i5"), listOf("8GB", "16GB"), listOf("128GB", "256GB", "512GB", "1.5TB")),
        LaptopPreset("Apple", "MacBook Pro 16-inch (2019)", 2019, listOf("Intel Core i7", "Intel Core i9"), listOf("16GB", "32GB", "64GB"), listOf("512GB", "1TB", "2TB", "4TB", "8TB")),
        LaptopPreset("Apple", "MacBook Air 13-inch (2020, M1)", 2020, listOf("Apple M1"), listOf("8GB", "16GB"), listOf("256GB", "512GB", "1TB", "2TB")),
        LaptopPreset("Apple", "MacBook Pro 13-inch (2020, M1)", 2020, listOf("Apple M1"), listOf("8GB", "16GB"), listOf("256GB", "512GB", "1TB", "2TB")),
        LaptopPreset("Apple", "MacBook Pro 14-inch (2021)", 2021, listOf("Apple M1 Pro", "Apple M1 Max"), listOf("16GB", "32GB", "64GB"), listOf("512GB", "1TB", "2TB", "4TB", "8TB")),
        LaptopPreset("Apple", "MacBook Air 13-inch (2022, M2)", 2022, listOf("Apple M2"), listOf("8GB", "16GB", "24GB"), listOf("256GB", "512GB", "1TB", "2TB")),
        LaptopPreset("Apple", "MacBook Pro 14-inch (2023)", 2023, listOf("Apple M2 Pro", "Apple M2 Max", "Apple M3", "Apple M3 Pro", "Apple M3 Max"), listOf("8GB", "16GB", "18GB", "24GB", "32GB", "36GB", "48GB", "64GB", "96GB", "128GB"), listOf("512GB", "1TB", "2TB", "4TB", "8TB")),
        LaptopPreset("Apple", "MacBook Air 13-inch (2024, M3)", 2024, listOf("Apple M3"), listOf("8GB", "16GB", "24GB"), listOf("256GB", "512GB", "1TB", "2TB")),
        LaptopPreset("Apple", "MacBook Pro 14-inch (2024)", 2024, listOf("Apple M4", "Apple M4 Pro", "Apple M4 Max"), listOf("16GB", "24GB", "32GB", "36GB", "48GB", "64GB", "128GB"), listOf("512GB", "1TB", "2TB", "4TB", "8TB")),
        LaptopPreset("Apple", "MacBook Air 13-inch (2025)", 2025, listOf("Apple M4"), listOf("16GB", "24GB", "32GB"), listOf("256GB", "512GB", "1TB", "2TB")),
        LaptopPreset("Apple", "MacBook Pro 14-inch (2025)", 2025, listOf("Apple M5"), listOf("16GB", "24GB", "32GB", "48GB", "64GB"), listOf("512GB", "1TB", "2TB", "4TB")),
        LaptopPreset("Apple", "MacBook Neo 13-inch (2026)", 2026, listOf("Apple A18 Pro"), listOf("8GB"), listOf("256GB", "512GB")),
        LaptopPreset("Apple", "MacBook Air 13-inch (2026, M5)", 2026, listOf("Apple M5"), listOf("16GB", "24GB", "32GB"), listOf("512GB", "1TB", "2TB")),
        LaptopPreset("Apple", "MacBook Air 15-inch (2026, M5)", 2026, listOf("Apple M5"), listOf("16GB", "24GB", "32GB"), listOf("512GB", "1TB", "2TB")),
        LaptopPreset("Apple", "MacBook Pro 14-inch (2026)", 2026, listOf("Apple M5", "Apple M5 Pro", "Apple M5 Max"), listOf("16GB", "24GB", "32GB", "36GB", "48GB", "64GB", "128GB"), listOf("512GB", "1TB", "2TB", "4TB", "8TB")),
        LaptopPreset("Apple", "MacBook Pro 16-inch (2026)", 2026, listOf("Apple M5 Pro", "Apple M5 Max"), listOf("24GB", "32GB", "36GB", "48GB", "64GB", "128GB"), listOf("1TB", "2TB", "4TB", "8TB"))
    )

    val presets: List<LaptopPreset> = (apple + ChromebookSelectionCatalog.presets + families.flatMap { family ->
        (family.from..family.to).map { year ->
            LaptopPreset(family.brand, "${family.name} ($year)", year, family.processors, modernRam, modernStorage)
        }
    }).filter { it.year in 2006..2026 }

    val brands: List<String> = presets.map { it.brand }.distinct().sorted()

    fun models(brand: String): List<LaptopPreset> = presets.filter { it.brand == brand }.sortedByDescending { it.year }

    fun canonicalQuery(preset: LaptopPreset, processor: String, ram: String, storage: String): String =
        listOf(preset.brand, preset.model, processor, ram, storage).filter { it.isNotBlank() }.joinToString(" ")
}
