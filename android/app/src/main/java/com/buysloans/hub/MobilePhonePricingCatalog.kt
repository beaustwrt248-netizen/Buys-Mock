package com.buysloans.hub

data class MobilePhonePriceEntry(
    val brand: String,
    val model: String,
    val storage: String,
    val priceSheetValue: Double
) {
    val displayName: String get() = "$model • $storage"
}

object MobilePhonePricingCatalog {
    const val APPLE = "Apple"

    val entries = listOf(
        MobilePhonePriceEntry(APPLE, "iPhone Air", "256 GB", 1399.0),
        MobilePhonePriceEntry(APPLE, "iPhone Air", "512 GB", 1799.0),
        MobilePhonePriceEntry(APPLE, "iPhone Air", "1 TB", 2199.0),

        MobilePhonePriceEntry(APPLE, "iPhone 17 Pro Max", "256 GB", 1899.0),
        MobilePhonePriceEntry(APPLE, "iPhone 17 Pro Max", "512 GB", 2299.0),
        MobilePhonePriceEntry(APPLE, "iPhone 17 Pro Max", "1 TB", 2799.0),
        MobilePhonePriceEntry(APPLE, "iPhone 17 Pro Max", "2 TB", 3399.0),
        MobilePhonePriceEntry(APPLE, "iPhone 17 Pro", "256 GB", 1699.0),
        MobilePhonePriceEntry(APPLE, "iPhone 17 Pro", "512 GB", 1999.0),
        MobilePhonePriceEntry(APPLE, "iPhone 17 Pro", "1 TB", 2399.0),
        MobilePhonePriceEntry(APPLE, "iPhone 17", "256 GB", 1199.0),
        MobilePhonePriceEntry(APPLE, "iPhone 17", "512 GB", 1499.0),

        MobilePhonePriceEntry(APPLE, "iPhone 16 Pro Max", "256 GB", 1649.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16 Pro Max", "512 GB", 1749.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16 Pro Max", "1 TB", 1999.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16 Pro", "128 GB", 1499.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16 Pro", "256 GB", 1599.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16 Pro", "512 GB", 1699.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16 Pro", "1 TB", 1799.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16 Plus", "128 GB", 999.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16 Plus", "256 GB", 1199.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16 Plus", "512 GB", 1499.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16", "128 GB", 949.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16", "256 GB", 1149.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16", "512 GB", 1349.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16e", "128 GB", 699.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16e", "256 GB", 849.0),
        MobilePhonePriceEntry(APPLE, "iPhone 16e", "512 GB", 1199.0),

        MobilePhonePriceEntry(APPLE, "iPhone 15 Pro Max", "256 GB", 1299.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15 Pro Max", "512 GB", 1399.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15 Pro Max", "1 TB", 1499.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15 Pro", "128 GB", 1199.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15 Pro", "256 GB", 1249.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15 Pro", "512 GB", 1399.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15 Plus", "128 GB", 949.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15 Plus", "256 GB", 1199.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15 Plus", "512 GB", 1299.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15", "128 GB", 849.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15", "256 GB", 949.0),
        MobilePhonePriceEntry(APPLE, "iPhone 15", "512 GB", 1099.0),

        MobilePhonePriceEntry(APPLE, "iPhone 14 Pro Max", "128 GB", 849.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14 Pro Max", "256 GB", 949.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14 Pro Max", "512 GB", 999.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14 Pro Max", "1 TB", 1099.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14 Pro", "128 GB", 799.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14 Pro", "256 GB", 899.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14 Pro", "512 GB", 949.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14 Pro", "1 TB", 999.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14 Plus", "128 GB", 699.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14 Plus", "256 GB", 799.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14 Plus", "512 GB", 899.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14", "128 GB", 649.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14", "256 GB", 749.0),
        MobilePhonePriceEntry(APPLE, "iPhone 14", "512 GB", 849.0),

        MobilePhonePriceEntry(APPLE, "iPhone 13 Pro Max", "128 GB", 699.0),
        MobilePhonePriceEntry(APPLE, "iPhone 13 Pro Max", "256 GB", 749.0),
        MobilePhonePriceEntry(APPLE, "iPhone 13 Pro Max", "512 GB", 799.0),
        MobilePhonePriceEntry(APPLE, "iPhone 13 Pro", "128 GB", 649.0),
        MobilePhonePriceEntry(APPLE, "iPhone 13 Pro", "256 GB", 699.0),
        MobilePhonePriceEntry(APPLE, "iPhone 13 Pro", "512 GB", 749.0),
        MobilePhonePriceEntry(APPLE, "iPhone 13", "128 GB", 499.0),
        MobilePhonePriceEntry(APPLE, "iPhone 13", "256 GB", 549.0),
        MobilePhonePriceEntry(APPLE, "iPhone 13", "512 GB", 649.0)
    )

    val grades = listOf("A", "B", "C")
    val gradeBuyPercent = mapOf(
        "A" to 0.70,
        "B" to 0.50,
        "C" to 0.30
    )

    fun brands(): List<String> = entries.map { it.brand }.distinct()
    fun models(brand: String): List<String> = entries.filter { it.brand == brand }.map { it.model }.distinct()
    fun variants(brand: String, model: String): List<MobilePhonePriceEntry> = entries.filter { it.brand == brand && it.model == model }

    fun buyPrice(entry: MobilePhonePriceEntry, grade: String): Double =
        entry.priceSheetValue * (gradeBuyPercent[grade] ?: error("Unsupported grade: $grade"))
}
