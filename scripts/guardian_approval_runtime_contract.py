from pathlib import Path

repairs = Path('admin/guardian-repairs.js').read_text(encoding='utf-8')
interactions = Path('admin/guardian-interactions-fix.js').read_text(encoding='utf-8')

# Second human approval must be recorded through the protected RPC before executor write work starts.
rpc = repairs.index("window.sb.rpc('guardian_decide_repair'")
executor = repairs.index("await executeApprovedRepair(repairId)", rpc)
assert rpc < executor, 'repair approval must be recorded before protected execution starts'

# Executor calls must carry the active Admin bearer token.
assert 'Authorization:`Bearer ${token}`' in repairs
assert "guardian-repair-executor" in repairs
assert 'Retry protected execution' in repairs

# The mobile performance regression came from an observer mutating the same subtree it observed.
assert 'MutationObserver' not in interactions, 'self-triggering Guardian MutationObserver must not return'
assert "window.addEventListener('guardian:repairs-rendered',scheduleEnhance)" in interactions

# Background work must back off and stop while the Admin page is hidden.
assert 'document.hidden' in interactions
assert 'document.hidden' in repairs
assert '45000' in repairs
assert '120000' in interactions

print('Guardian approval/performance runtime contract: PASS')
