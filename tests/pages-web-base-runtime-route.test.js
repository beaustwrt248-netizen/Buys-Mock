'use strict';

const fs = require('fs');
const assert = require('assert');

const index = fs.readFileSync('index.html', 'utf8');
const workflow = fs.readFileSync('.github/workflows/deploy-admin-pages.yml', 'utf8');

assert(
  index.includes("const CANDIDATE='web-base.html'"),
  'production bootstrap must keep web-base.html as the plain runtime route'
);
assert(
  index.includes("fetch(CANDIDATE,{cache:'no-store'})"),
  'production bootstrap must fetch the local runtime base without a cache-busting query contract'
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
  ['$BASE/mobile-layout-fix.js', 'home|computer|console|general'],
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
    workflow.includes(`fetch_until_contains "${url}"` ) && workflow.includes(marker),
    `Pages smoke must use content-aware retries for ${url} :: ${marker}`
  );
}
assert(
  !workflow.includes('fetch_with_retry "$BASE/" "$RUNNER_TEMP/page-index.html"'),
  'Pages smoke must not treat an HTTP 200 with stale index content as deployment success'
);
assert(
  !workflow.includes('fetch_with_retry "$BASE/secure-pricing.js"'),
  'runtime-critical asset checks must not treat an HTTP 200 with stale content as deployment success'
);

console.log('Pages runtime route and content-aware propagation contracts verified');
