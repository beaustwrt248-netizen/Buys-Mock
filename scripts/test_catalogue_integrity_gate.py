#!/usr/bin/env python3
import unittest
from catalogue_integrity_gate import validate

class CatalogueIntegrityGateTests(unittest.TestCase):
    def test_valid_australian_variant(self):
        rows=[{"category":"mobile_phone","brand":"Samsung","model_name":"Galaxy S24","model_number":"SM-S921B","storage_options":["128GB","256GB"],"network_generation":"5G"}]
        self.assertTrue(validate(rows)[0]["valid"])
    def test_carrier_brand_and_3g_only_fail(self):
        rows=[{"category":"mobile_phone","brand":"Telstra","model_name":"Legacy Phone","model_number":"T1","storage_options":["32GB"],"network_generation":"3G"}]
        codes={x["code"] for x in validate(rows)[0]["issues"]}; self.assertIn("carrier_as_brand",codes); self.assertIn("3g_only",codes)
    def test_duplicate_exact_variant_fails_second_only(self):
        row={"category":"tablet","brand":"Apple","model_name":"iPad","model_number":"A2696","storage_options":["64GB"],"network_generation":"5G"}
        result=validate([row,dict(row)]); self.assertTrue(result[0]["valid"]); self.assertIn("duplicate_variant",{x["code"] for x in result[1]["issues"]})
    def test_storage_variants_remain_distinct(self):
        base={"category":"console","brand":"Sony","model_name":"PlayStation 5","model_number":"CFI-2002","storage_options":["1TB"]}
        other=dict(base,storage_options=["825GB"]); self.assertTrue(all(x["valid"] for x in validate([base,other])))

if __name__=="__main__": unittest.main()
