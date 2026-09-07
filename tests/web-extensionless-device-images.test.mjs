import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../morley-central-pricing.js', import.meta.url), 'utf8');

assert.match(source, /function normalizeWebImageUrl\(raw\)/, 'web catalogue must normalize verified extensionless image URLs');
assert.match(source, /lh3\.googleusercontent\.com/, 'Google Store image CDN must be supported');
assert.match(source, /cdn\.uc\.assets\.prezly\.com/, 'Acer press image CDN must be supported');
assert.match(source, /cdn-dynmedia-1\.microsoft\.com/, 'Microsoft image CDN must be supported');
assert.match(source, /press\.asus\.com/, 'ASUS press image host must be supported');
assert.match(source, /u\.hash='morley\.jpg'/, 'extensionless images must gain a fragment-only image suffix without changing the HTTP resource');
assert.match(source, /const imageUrl=normalizeWebImageUrl\(device\.image_reference_url\)/, 'catalogue rows must use normalized web image URLs');
assert.match(source, /MorleyCentralPricing=\{version:6,/, 'pricing runtime version must advance when image normalization changes');

console.log('web extensionless device image contract OK');
