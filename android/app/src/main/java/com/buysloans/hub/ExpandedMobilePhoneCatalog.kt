package com.buysloans.hub

/**
 * Additive catalogue growth beyond the original imported device list.
 *
 * These rows intentionally contain no Morley pricing. MobilePhoneDeviceCatalog merges them
 * behind the authoritative price sheet, so an existing priced brand/model/storage entry is
 * never overwritten by catalogue expansion.
 */
data class ExpandedMobilePhoneModel(
    val brand: String,
    val model: String,
    val modelNumber: String? = null,
    val storages: List<String>
)

object ExpandedMobilePhoneCatalog {
    val models = listOf(
        // Apple generations not covered by the current Morley priced sheet.
        ExpandedMobilePhoneModel("Apple", "iPhone 12 mini", storages = listOf("64 GB", "128 GB", "256 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone 12", storages = listOf("64 GB", "128 GB", "256 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone 12 Pro", storages = listOf("128 GB", "256 GB", "512 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone 12 Pro Max", storages = listOf("128 GB", "256 GB", "512 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone 11", storages = listOf("64 GB", "128 GB", "256 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone 11 Pro", storages = listOf("64 GB", "256 GB", "512 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone 11 Pro Max", storages = listOf("64 GB", "256 GB", "512 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone XS", storages = listOf("64 GB", "256 GB", "512 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone XS Max", storages = listOf("64 GB", "256 GB", "512 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone XR", storages = listOf("64 GB", "128 GB", "256 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone X", storages = listOf("64 GB", "256 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone 8", storages = listOf("64 GB", "256 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone 8 Plus", storages = listOf("64 GB", "256 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone SE (2nd generation)", storages = listOf("64 GB", "128 GB", "256 GB")),
        ExpandedMobilePhoneModel("Apple", "iPhone SE (3rd generation)", storages = listOf("64 GB", "128 GB", "256 GB")),

        // Additional Samsung flagship generations.
        ExpandedMobilePhoneModel("Samsung", "Galaxy S21", "SM-G991B", listOf("128 GB", "256 GB")),
        ExpandedMobilePhoneModel("Samsung", "Galaxy S21 Plus", "SM-G996B", listOf("128 GB", "256 GB")),
        ExpandedMobilePhoneModel("Samsung", "Galaxy S21 Ultra", "SM-G998B", listOf("128 GB", "256 GB", "512 GB")),
        ExpandedMobilePhoneModel("Samsung", "Galaxy S20", "SM-G980F", listOf("128 GB")),
        ExpandedMobilePhoneModel("Samsung", "Galaxy S20 Plus", "SM-G985F", listOf("128 GB")),
        ExpandedMobilePhoneModel("Samsung", "Galaxy S20 Ultra", "SM-G988B", listOf("128 GB", "512 GB")),
        ExpandedMobilePhoneModel("Samsung", "Galaxy Note20", "SM-N980F", listOf("256 GB")),
        ExpandedMobilePhoneModel("Samsung", "Galaxy Note20 Ultra", "SM-N985F", listOf("256 GB", "512 GB")),

        // New brand families so the catalogue can keep growing without changing pricing logic.
        ExpandedMobilePhoneModel("HONOR", "Magic7 Pro", storages = listOf("512 GB")),
        ExpandedMobilePhoneModel("HONOR", "Magic V3", storages = listOf("512 GB")),
        ExpandedMobilePhoneModel("HONOR", "200 Pro", storages = listOf("512 GB")),
        ExpandedMobilePhoneModel("HONOR", "200", storages = listOf("256 GB", "512 GB")),
        ExpandedMobilePhoneModel("HONOR", "90", storages = listOf("256 GB", "512 GB")),
        ExpandedMobilePhoneModel("HONOR", "X8b", storages = listOf("256 GB", "512 GB")),

        ExpandedMobilePhoneModel("ASUS", "ROG Phone 9 Pro", storages = listOf("512 GB", "1 TB")),
        ExpandedMobilePhoneModel("ASUS", "ROG Phone 9", storages = listOf("256 GB", "512 GB")),
        ExpandedMobilePhoneModel("ASUS", "Zenfone 12 Ultra", storages = listOf("256 GB", "512 GB")),
        ExpandedMobilePhoneModel("ASUS", "Zenfone 11 Ultra", storages = listOf("256 GB", "512 GB")),
        ExpandedMobilePhoneModel("ASUS", "ROG Phone 8 Pro", storages = listOf("512 GB", "1 TB")),

        ExpandedMobilePhoneModel("HMD", "Skyline", storages = listOf("128 GB", "256 GB")),
        ExpandedMobilePhoneModel("HMD", "Fusion", storages = listOf("128 GB", "256 GB")),
        ExpandedMobilePhoneModel("HMD", "Pulse Pro", storages = listOf("128 GB")),
        ExpandedMobilePhoneModel("HMD", "XR21", storages = listOf("128 GB")),

        ExpandedMobilePhoneModel("Nokia", "X30 5G", storages = listOf("128 GB", "256 GB")),
        ExpandedMobilePhoneModel("Nokia", "G60 5G", storages = listOf("64 GB", "128 GB")),
        ExpandedMobilePhoneModel("Nokia", "G42 5G", storages = listOf("128 GB", "256 GB")),

        ExpandedMobilePhoneModel("Fairphone", "Fairphone 5", storages = listOf("256 GB")),
        ExpandedMobilePhoneModel("Fairphone", "Fairphone 4", storages = listOf("128 GB", "256 GB")),

        ExpandedMobilePhoneModel("Meizu", "21 Pro", storages = listOf("256 GB", "512 GB", "1 TB")),
        ExpandedMobilePhoneModel("Meizu", "21", storages = listOf("256 GB", "512 GB")),
        ExpandedMobilePhoneModel("Meizu", "20 Pro", storages = listOf("128 GB", "256 GB", "512 GB"))
    )
}
