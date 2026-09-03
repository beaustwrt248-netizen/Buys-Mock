'use strict';

const fs = require('fs');
const assert = require('assert');

const index = fs.readFileSync('index.html', 'utf8');
const workflow = fs.readFileSync('.github/workflows/deploy-admin-pages.yml', 'utf8');
const mobileLayout = fs.readFileSync('mobile-layout-fix.js', 'utf8');

assert(
  index.includes("const CANDIDATE='web-base.html'"),
  'production bootstrap must keep web-base.html as the plain runtime route'
);
assert(
  index.includes('attempt===0?CANDIDATE'),
  'first workspace request must use the exact plain web-base.html runtime route'
);
assert(
  index.includes("fetch(url,{cache:'no-store'})"),
  'workspace bootstrap must bypass the browser cache on every attempt'
);
assert(
  index.includes('attempt<5'),
  'workspace bootstrap must retry bounded transient failures before showing the hard error'
);
assert(
  index.includes("CANDIDATE+'?morley_retry='"),
  'retry attempts must be able to bypass a stale custom-domain/CDN edge object'
);
assert(
  index.includes("BASE_MARKER='Buys and Loans Hub'") && index.includes('h.includes(BASE_MARKER)'),
  'workspace bootstrap must reject a successful HTTP response that is not the expected base document'
);
assert(
  index.includes("throw new Error('Could not load the Morley workspace')"),
  'workspace bootstrap must remain a hard failure after the bounded retry window'
);
assert(
  workflow.includes('fetch_runtime_until_contains "$BASE/web-base.html" "$RUNNER_TEMP/web-base.html" \'Buys and Loans Hub\''),
  'Pages smoke must validate the exact plain web-base.html runtime URL'
);
assert(
  !workflow.includes('$BASE/web-base.html?morley_sha=${GITHUB_SHA}'),
  'Pages smoke must not mutate the runtime-critical web-base.html URL with a query string'
);
assert(
  workflow.includes('::error::Exact runtime URL did not expose expected deployed content'),
  'runtime-route smoke must remain a hard failure with an actionable diagnostic'
);
for (const marker of [
  "const CANDIDATE='web-base.html'",
  'secure-pricing.js',
  'mobile-parity-v3.css',
  'morley-light-web.css',
  'mobile-layout-fix.js'
]) {
  assert(
    workflow.includes(`fetch_until_contains "$BASE/" "$RUNNER_TEMP/page-index.html" '${marker}'`) ||
      workflow.includes(`fetch_until_contains "$BASE/" "$RUNNER_TEMP/page-index.html" "${marker}"`),
    `Pages smoke must retry the deployed index until ${marker} reaches the custom domain`
  );
}
for (const contract of [
  ['$BASE/secure-pricing.js', 'localStorage.getItem(STORE)'],
  ['$BASE/product-parity-v3.js', 'Computer Pricing'],
  ['$BASE/ultimate-parity.js', 'Help & FAQ'],
  ['$BASE/mobile-layout-fix.js', 'home|laptop|general|settings'],
  ['$BASE/mobile-parity-v3.css', '@media(max-width:760px)'],
  ['$BASE/morley-light-web.css', 'color-scheme:light'],
  ['$BASE/desktop-oem.js', "localStorage.getItem('morley_web_auth')"],
  ['$BASE/no-gold.css', '--yellow:#2f7cff'],
  ['$BASE/web-a11y.js', 'focus-visible'],
  ['$BASE/desktop-parity.js', 'morley-section-back'],
  ['$BASE/admin/turnstile.html', 'postMessage(payload,postMessageOrigin)'],
  ['$BASE/admin/index.html', 'styles.css?v=3'],
  ['$BASE/admin/invites.js', 'display_name:name'],
  ['$BASE/admin/app.js', 'admin-user-control']
]) {
  const [url, marker] = contract;
  assert(
    workflow.includes(`fetch_until_contains "${url}"`) && workflow.includes(marker),
    `Pages smoke must use content-aware retries for ${url} :: ${marker}`
  );
}
assert(
  mobileLayout.includes("const signature='home|laptop|general|settings'"),
  'Pages mobile navigation marker must track the current four-item mobile navigation contract'
);
assert(
  !workflow.includes('home|computer|console|general'),
  'Pages smoke must not retain the stale pre-categories mobile navigation marker'
);
assert(
  !workflow.includes('fetch_with_retry "$BASE/" "$RUNNER_TEMP/page-index.html"'),
  'Pages smoke must not treat an HTTP 200 with stale index content as deployment success'
);
assert(
  !workflow.includes('fetch_with_retry "$BASE/secure-pricing.js"'),
  'runtime-critical asset checks must not treat an HTTP 200 with stale content as deployment success'
);

console.log('Pages runtime route, resilient bootstrap, and propagation contracts verified');
