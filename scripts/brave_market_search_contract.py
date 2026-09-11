from pathlib import Path

root = Path(__file__).resolve().parents[1]
edge = (root / "supabase/functions/market-search-v2/index.ts").read_text(encoding="utf-8")
android = (root / "android/app/src/main/java/com/buysloans/hub/LaptopGuidedScreen.kt").read_text(encoding="utf-8")

required_edge = [
    'Deno.env.get("BRAVE_SEARCH_API_KEY")',
    '"X-Subscription-Token": key',
    'u.searchParams.set("country", "AU")',
    'retail: ["brave", "serpapi-google-shopping"]',
    'marketplaces: ["brave", "serpapi"]',
    'site:gumtree.com.au',
    'site:facebook.com/marketplace/item',
    'userClient.auth.getUser(accessToken)',
    'if (!accessToken) return reply({ error: "Authentication required" }, 401)',
    'const ALLOWED_ORIGINS = new Set(["https://buyshub.me", "https://www.buyshub.me", "https://beaustwrt248-netizen.github.io"])',
    'ALLOWED_ORIGINS.has(origin) ? origin : "https://buyshub.me"',
    '"Cache-Control": "no-store"',
    'pricingAnalysis',
    'analyseUsedMarket',
    'applyUsedBounds',
    'outlierFilter: "IQR-1.5"',
    'retailExcludedFromUsedMedian: true',
]
for marker in required_edge:
    assert marker in edge, f"missing Brave market-search contract marker: {marker}"

assert "BRAVE_SEARCH_API_KEY=" not in edge, "Brave API key must never be committed"
assert 'req.headers.get("origin") || ORIGIN' not in edge, "CORS must not reflect arbitrary caller origins"
assert 'req.headers.get("Origin") || ORIGIN' not in edge, "CORS must not reflect arbitrary caller origins"
assert '"Access-Control-Allow-Origin": origin' not in edge, "CORS must not reflect arbitrary caller origins without allowlist validation"
assert "https://buyshub.me" in edge and "https://www.buyshub.me" in edge and "https://beaustwrt248-netizen.github.io" in edge, "All approved production origins must remain explicit"
assert "functions/v1/market-search-v2" in android, "Laptop flow must use Brave-first market search endpoint"
assert 'setRequestProperty("Authorization", "Bearer $token")' in android, "Android market search must forward the signed-in Supabase session"
assert "Google/eBay:" not in android, "Provider attribution must not hard-code Google when Brave is primary"
assert "Google Shopping fallback" in android, "SerpApi/Google Shopping must be labelled as fallback"
assert "Gumtree" in android and "Facebook" in android, "Marketplace discovery counts must remain visible"

print("Brave-first market search + pricing confidence contract: PASS")
