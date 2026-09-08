#!/usr/bin/env python3
"""Regression coverage for the guarded Nova OTA metadata contract."""

import unittest

from nova_ota_metadata_contract import parse_source_identity, validate_metadata


REPOSITORY = "beaustwrt248-netizen/Buys-Mock"
SHA256 = "52b4c23a82fa49e66b3301d5b57c0d6c8766ba7361e26a5779943323ee413593"


def valid_metadata():
    return {
        "versionCode": 25,
        "versionName": "0.3.21",
        "apkUrl": (
            "https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/"
            "nova-v0.3.21/Nova-AI-0.3.21.apk"
        ),
        "sha256": SHA256,
        "mandatory": False,
    }


class NovaOtaMetadataContractTests(unittest.TestCase):
    def test_accepts_exact_source_and_release_identity(self):
        code, name = parse_source_identity(
            "defaultConfig { versionCode 25\nversionName '0.3.21' }"
        )
        self.assertEqual(
            validate_metadata(code, name, valid_metadata(), REPOSITORY),
            {
                "NOVA_RELEASE_TAG": "nova-v0.3.21",
                "NOVA_RELEASE_FILE": "Nova-AI-0.3.21.apk",
                "NOVA_EXPECTED_SHA256": SHA256,
            },
        )

    def test_rejects_source_manifest_mismatch(self):
        metadata = valid_metadata()
        metadata["versionCode"] = 24
        with self.assertRaisesRegex(ValueError, "must match Nova source exactly"):
            validate_metadata(25, "0.3.21", metadata, REPOSITORY)

    def test_rejects_non_integer_version_code(self):
        metadata = valid_metadata()
        metadata["versionCode"] = "25"
        with self.assertRaisesRegex(ValueError, "must match Nova source exactly"):
            validate_metadata(25, "0.3.21", metadata, REPOSITORY)

    def test_rejects_uppercase_digest(self):
        metadata = valid_metadata()
        metadata["sha256"] = SHA256.upper()
        with self.assertRaisesRegex(ValueError, "lowercase hexadecimal"):
            validate_metadata(25, "0.3.21", metadata, REPOSITORY)

    def test_rejects_non_boolean_mandatory(self):
        metadata = valid_metadata()
        metadata["mandatory"] = "false"
        with self.assertRaisesRegex(ValueError, "must be a boolean"):
            validate_metadata(25, "0.3.21", metadata, REPOSITORY)

    def test_rejects_untrusted_release_url(self):
        metadata = valid_metadata()
        metadata["apkUrl"] = "https://example.invalid/Nova-AI-0.3.21.apk"
        with self.assertRaisesRegex(ValueError, "must exactly match"):
            validate_metadata(25, "0.3.21", metadata, REPOSITORY)

    def test_rejects_unsafe_source_version_name(self):
        with self.assertRaisesRegex(ValueError, "unsafe release-path characters"):
            parse_source_identity(
                "defaultConfig { versionCode 25\nversionName '0.3.21/other' }"
            )


if __name__ == "__main__":
    unittest.main()
