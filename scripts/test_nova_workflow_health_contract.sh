#!/usr/bin/env bash
set -euo pipefail
python3 scripts/test_nova_workflow_health_contract.py
node scripts/test_nova_workflow_health_behavior.mjs
node --check nova/app-core.js
