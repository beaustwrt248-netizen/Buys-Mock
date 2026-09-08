#!/usr/bin/env python3
"""Regression coverage for Nova's published release-asset contract."""

import unittest

from nova_release_asset_contract import validate_release_asset


ASSET = "Nova-AI-0.3.21.apk"
SHA256 = "52b4c23a82fa49e66b3301d5b57c0d6c8766ba7361e26a5779943323ee413593"


def valid_release():
    return {
        "assets": [
            {
                "name": ASSET,
                "digest": f"sha256:{SHA256}",
                "size": 5_522_993,
            }
        ]
    }


class NovaReleaseAssetContractTests(unittest.TestCase):
    def test_accepts_one_exact_non_empty_asset(self):
        self.assertEqual(
            validate_release_asset(valid_release(), ASSET, SHA256),
            5_522_993,
        )

    def test_rejects_missing_asset(self):
        with self.assertRaisesRegex(ValueError, "found 0"):
            validate_release_asset({"assets": []}, ASSET, SHA256)

    def test_rejects_duplicate_asset_name(self):
        release = valid_release()
        release["assets"].append(dict(release["assets"][0]))
        with self.assertRaisesRegex(ValueError, "found 2"):
            validate_release_asset(release, ASSET, SHA256)

    def test_rejects_digest_mismatch(self):
        release = valid_release()
        release["assets"][0]["digest"] = f"sha256:{'0' * 64}"
        with self.assertRaisesRegex(ValueError, "digest does not match"):
            validate_release_asset(release, ASSET, SHA256)

    def test_rejects_empty_asset(self):
        release = valid_release()
        release["assets"][0]["size"] = 0
        with self.assertRaisesRegex(ValueError, "positive byte size"):
            validate_release_asset(release, ASSET, SHA256)

    def test_rejects_malformed_release_response(self):
        with self.assertRaisesRegex(ValueError, "assets array"):
            validate_release_asset({}, ASSET, SHA256)


if __name__ == "__main__":
    unittest.main()
