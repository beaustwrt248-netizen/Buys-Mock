import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
import catalogue_import_validator as validator  # noqa: E402


class CatalogueImportValidatorTests(unittest.TestCase):
    def valid_row(self, **overrides):
        row = {
            "id": 1,
            "category": "mobile_phone",
            "brand": "Samsung",
            "model_name": "Galaxy S24 Ultra",
            "model_number": "SM-S928B",
            "storage_options": ["256GB", "512GB"],
            "source_url": "https://www.samsung.com/au/",
        }
        row.update(overrides)
        return row

    def codes(self, row):
        return {issue.code for issue in validator.validate_row(row)}

    def test_accepts_clean_canonical_row(self):
        self.assertEqual(self.codes(self.valid_row()), set())

    def test_rejects_battery_health_listing_suffix(self):
        self.assertIn(
            "listing_text_in_model_name",
            self.codes(self.valid_row(model_name="iPhone 16 Pro Max 256GB - 81%")),
        )

    def test_rejects_condition_and_sold_as_is_text(self):
        for name in (
            "iPhone 13 Mini 128GB - Cracked Screen",
            "iPhone 13 Pro 128GB - Sold As Is (No Warranty)",
            "iPhone 13 Pro Max - Doesn't Turn On",
            "iPhone 11 - Can't Update from iOS 16",
        ):
            with self.subTest(name=name):
                self.assertIn("listing_text_in_model_name", self.codes(self.valid_row(model_name=name)))

    def test_storage_title_requires_structured_storage(self):
        self.assertIn(
            "storage_not_structured",
            self.codes(self.valid_row(model_name="Galaxy S24 Ultra 512GB", storage_options=[])),
        )

    def test_storage_title_is_allowed_when_structured(self):
        self.assertNotIn(
            "storage_not_structured",
            self.codes(self.valid_row(model_name="Galaxy S24 Ultra 512GB", storage_options=["512GB"])),
        )

    def test_missing_model_number_is_reported_not_guessed(self):
        self.assertIn("missing_model_number", self.codes(self.valid_row(model_number="")))

    def test_wearable_storage_is_not_required(self):
        self.assertNotIn(
            "storage_not_structured",
            self.codes(
                self.valid_row(
                    category="wearable",
                    model_name="Galaxy Watch7",
                    model_number="SM-L300",
                    storage_options=[],
                )
            ),
        )


if __name__ == "__main__":
    unittest.main()
