import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const css = fs.readFileSync(path.join(root, 'completion.css'), 'utf8');

// The shell is a flex column: page-stack already occupies the space between
// the top bar and bottom navigation. Active pages must not reserve the nav a
// second time or content appears vertically clipped/over-padded on phones.
assert.match(
  css,
  /\.page\.is-active\s*\{[\s\S]*?padding-bottom:\s*calc\(var\(--nova-safe-bottom\) \+ 24px\)/,
  'active pages should reserve only content breathing room + safe area inside page-stack'
);
assert.doesNotMatch(
  css,
  /\.page\.is-active\s*\{[\s\S]*?padding-bottom:\s*calc\(var\(--nova-bottom-nav-height\)/,
  'active pages must not double-reserve the flex-sibling bottom navigation'
);

// Chat lives inside page-stack as well, so its layout should consume the full
// available page height rather than subtracting bottom navigation again.
assert.match(css, /\.chat-layout\s*\{[\s\S]*?height:\s*100%/);
assert.doesNotMatch(
  css,
  /\.chat-layout\s*\{[\s\S]*?height:\s*calc\(100% - var\(--nova-bottom-nav-height\)/,
  'chat must not double-subtract bottom navigation height'
);

// Filter rows must remain reachable at all supported phone widths, including
// the 390-430px widths from the mobile visual acceptance sweep.
assert.match(
  css,
  /@media\s*\(max-width:\s*430px\)[\s\S]*?\.filter-tabs\s*\{[\s\S]*?overflow-x:\s*auto/,
  'mobile filter tabs should scroll rather than clip through 430px widths'
);

console.log('mobile-shell-regression: ok');
