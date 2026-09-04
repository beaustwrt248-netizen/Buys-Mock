'use strict';
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = fs.readFileSync('desktop-parity.js', 'utf8');
const shown = [], saved = [], history = [];
const context = {
 document: {readyState:'loading', addEventListener(){}, querySelectorAll(){return [];}},
 window: {show(page){shown.push(page);}, scrollTo(){}},
 localStorage: {setItem(key,page){saved.push([key,page]);}},
 location: {hash:'#home'},
 history: {pushState(state,title,hash){history.push(hash);}, replaceState(){}},
};
vm.runInNewContext(source, context);
context.window.morleyDesktopGo('categories');
assert.deepEqual(shown, ['categories'], 'Categories button must display its destination');
assert.deepEqual(saved, [['morley_desktop_route','categories']]);
assert.deepEqual(history, ['#categories']);
context.window.morleyDesktopGo('invalid-route');
assert.equal(shown.length,1,'unknown routes remain rejected');
console.log('Categories navigation, persistence and history pass');
