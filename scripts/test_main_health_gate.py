#!/usr/bin/env python3
import importlib.util
import sys
from pathlib import Path

MODULE = Path(__file__).with_name("main_health_gate.py")
spec = importlib.util.spec_from_file_location("main_health_gate", MODULE)
m = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = m
spec.loader.exec_module(m)

SHA = "a" * 40

def run(evidence, required=("Quality",), changed=None):
    return m.classify(SHA, evidence, list(required), changed or [])

def check(condition, message):
    if not condition:
        raise AssertionError(message)

ready, v = run([{"name":"Quality","head_sha":SHA,"conclusion":"success","id":1}])
check(ready and v[0].status == "pass", "exact SHA success must pass")

ready, v = run([{"name":"Quality","head_sha":"b"*40,"conclusion":"success"}])
check(not ready and v[0].reason == "stale-sha evidence", "stale SHA must fail closed")

ready, v = run([])
check(not ready and v[0].reason == "missing evidence", "missing evidence must fail closed")

ready, v = run([{"name":"Quality","head_sha":SHA,"conclusion":"cancelled"}])
check(not ready and v[0].status == "blocked", "cancelled workflow must block")

ready, v = run([{"name":"Nova APK Build","head_sha":SHA,"conclusion":"skipped"}], required=("Nova APK Build",), changed=["admin/styles.css"])
check(ready and v[0].status == "not_applicable", "unrelated APK skip should be explicit N/A")

ready, v = run([{"name":"Nova APK Build","head_sha":SHA,"conclusion":"skipped"}], required=("Nova APK Build",), changed=["nova/app/build.gradle"])
check(not ready and v[0].reason == "unexpected skip", "relevant APK skip must block")

ready, v = run([{"name":"Nova PR Guard","head_sha":SHA,"conclusion":"failure"}], required=("Nova PR Guard",))
check(not ready and v[0].status == "protected_block", "protected approval failure must remain protected")

print("main health gate regression tests passed")
