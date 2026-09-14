import assert from 'node:assert/strict';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const core = require('../catalogue-connectivity-core.js');

const tabletCases = [
  ['iPad (10th generation) Wi-Fi', 'iPad (10th generation)', 'Wi-Fi'],
  ['iPad (10th generation) Wi-Fi + Cellular', 'iPad (10th generation)', 'Wi-Fi + Cellular'],
  ['Galaxy Tab S10+ 5G', 'Galaxy Tab S10+', '5G'],
  ['Galaxy Tab S9 FE 4G', 'Galaxy Tab S9 FE', '4G'],
  ['Lenovo Tab P12 Cellular', 'Lenovo Tab P12', 'Cellular']
];
for (const [model, baseModel, connectivity] of tabletCases) {
  const parsed = core.parseVariant('tablet', model);
  assert.equal(parsed.baseModel, baseModel, model);
  assert.equal(parsed.connectivity, connectivity, model);
}

const watchCases = [
  ['Apple Watch Series 10 46mm GPS', 'Apple Watch Series 10 46mm', 'GPS'],
  ['Apple Watch Series 10 46mm GPS + Cellular', 'Apple Watch Series 10 46mm', 'GPS + Cellular'],
  ['Apple Watch Series 10 46mm GPS + LTE', 'Apple Watch Series 10 46mm', 'GPS + Cellular'],
  ['Galaxy Watch7 44mm LTE', 'Galaxy Watch7 44mm', 'LTE'],
  ['Pixel Watch 3 45mm Cellular', 'Pixel Watch 3 45mm', 'Cellular'],
  ['Galaxy Watch Ultra 47mm Bluetooth', 'Galaxy Watch Ultra 47mm', 'Bluetooth']
];
for (const [model, baseModel, connectivity] of watchCases) {
  const parsed = core.parseVariant('wearable', model);
  assert.equal(parsed.baseModel, baseModel, model);
  assert.equal(parsed.connectivity, connectivity, model);
}

assert.notEqual(
  core.groupKey({ category:'wearable', brand:'Apple', model:'Apple Watch Series 10 42mm GPS' }),
  core.groupKey({ category:'wearable', brand:'Apple', model:'Apple Watch Series 10 46mm GPS' }),
  'watch sizes must stay separate'
);
assert.notEqual(
  core.groupKey({ category:'wearable', brand:'Apple', model:'Apple Watch Series 9 46mm GPS' }),
  core.groupKey({ category:'wearable', brand:'Apple', model:'Apple Watch Series 10 46mm GPS' }),
  'watch generations must stay separate'
);
assert.notEqual(
  core.groupKey({ category:'wearable', brand:'Apple', model:'Apple Watch Series 10 46mm Titanium GPS + Cellular' }),
  core.groupKey({ category:'wearable', brand:'Apple', model:'Apple Watch Series 10 46mm GPS + Cellular' }),
  'edition/material identity must stay separate'
);

assert.equal(core.connectivityMatches('wearable', 'GPS + Cellular', 'lte'), true);
assert.equal(core.connectivityMatches('wearable', 'GPS + Cellular', 'cellular'), true);
assert.equal(core.connectivityMatches('wearable', 'GPS + Cellular', 'gps+lte'), true);
assert.equal(core.connectivityMatches('tablet', 'Wi-Fi + Cellular', 'wifi cellular'), true);
assert.equal(core.connectivityMatches('tablet', '5G', 'cellular'), true);

console.log('catalogue connectivity grouping: PASS');
