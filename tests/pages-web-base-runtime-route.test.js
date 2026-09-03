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

console.log('Pages web-base runtime route contract verified');
