import fs from 'node:fs';
import assert from 'node:assert/strict';

const src=fs.readFileSync('mobile-phone-grouping.js','utf8');

assert.match(src,/const storageFromPart=/);
assert.match(src,/internal\\s\+storage\|device\\s\+storage\|storage\|capacity\|rom/);
assert.match(src,/ram\|memory\\s\+ram\|expandable\|micro\\s\*sd\|microsd\|sd\\s\*card/);
assert.match(src,/const bare=text\.match\(\/\^\\s\*/);
assert.match(src,/const storageFromCard=card=>\{for\(const part of partsFromCard\(card\)\)\{const storage=storageFromPart\(part\)/);
assert.doesNotMatch(src,/part\.match\(\/\\b\(\\d\+\(\?:\\\.\\d\+\)\?\)\\s\*\(TB\|GB\|MB\)/);
