import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
import catalogue_cleanup_audit as audit  # noqa: E402


class CatalogueCleanupAuditTests(unittest.TestCase):
    def row(self, **overrides):
        value = {
            "id": 1,
            "category": "mobile_phone",
            "brand": "Samsung",
            "model_name": "Galaxy S24 Ultra",
            "model_number": "SM-S928B",
            "storage_options": ["256GB"],
            "source_name": "Samsung Australia",
            "source_url": "https://www.samsung.com/au/",
        }
        value.update(overrides)
        return value

    def test_clean_manufacturer_row_is_clean(self):
        result = audit.classify_row(self.row())
        self.assertEqual(result["classification"], "canonical_clean")

    def test_retailer_only_clean_shape_is_retained_for_verification(self):
        result = audit.classify_row(self.row(source_name="Cash Converters Australia"))
        self.assertEqual(result["classification"], "retailer_only_evidence")
        self.assertIn("retailer_only_identity_evidence", result["reasons"])

    def test_listing_text_is_high_confidence_repair_candidate(self):
        result = audit.classify_row(
            self.row(
                model_name="iPhone 13 Pro Max 128GB - 86%",
                brand="Apple",
                model_number="MLL93X/A",
                source_name="Cash Converters Australia",
            )
        )
        self.assertEqual(result["classification"], "high_confidence_repair_candidate")
        self.assertIn("listing_text_in_model_name", result["reasons"])

    def test_model_number_annotation_is_high_confidence_repair_candidate(self):
        for model_number in (
            "SM-A125F - INC BATTERY",
            "SM-A236B - CHROME",
            "SM-S906E - Bx168",
            "SM-F721B - 93%",
            "CPH2305 - WITH ATTACHED COVER",
            "XT2347-2 Patt",
            "TA-1234 - MIL",
        ):
            with self.subTest(model_number=model_number):
                result = audit.classify_row(
                    self.row(model_number=model_number, source_name="Cash Converters Australia")
                )
                self.assertEqual(result["classification"], "high_confidence_repair_candidate")
                self.assertIn("suspicious_model_number", result["reasons"])

    def test_missing_model_number_requires_verification(self):
        result = audit.classify_row(self.row(model_number=""))
        self.assertEqual(result["classification"], "metadata_verification_required")
        self.assertIn("missing_model_number", result["reasons"])


if __name__ == "__main__":
    unittest.main()
