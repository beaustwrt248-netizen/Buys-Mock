import assert from 'node:assert/strict';
import * as routerModule from '../src/router.mjs';

const pushes = [];
const replaces = [];
let replay = null;
let unsubscribed = false;
const historyAdapter = {
  read: () => '#/projects',
  push: route => pushes.push(route),
  replace: route => replaces.push(route),
  subscribe: handler => {
    replay = handler;
    return () => { unsubscribed = true; };
  }
};

const router = routerModule.createRouter({ initialRoute: 'home', history: historyAdapter });
assert.equal(router.current(), 'projects', 'initial route should restore from the URL adapter');
assert.deepEqual(replaces, ['projects'], 'initial route should be canonicalised without adding history');

router.go('chat');
assert.deepEqual(pushes, ['chat'], 'user navigation should add one history entry');
router.go('chat');
assert.deepEqual(pushes, ['chat'], 'same-route navigation should not duplicate history');

replay('#/settings');
assert.equal(router.current(), 'settings', 'Back/Forward replay should restore route state');
assert.deepEqual(pushes, ['chat'], 'Back/Forward replay must not push another history entry');

router.destroy();
assert.equal(unsubscribed, true, 'router cleanup should remove the browser listener');

assert.equal(typeof routerModule.createBrowserHistoryAdapter, 'function', 'browser adapter factory should exist');
const events = new Map();
const fakeWindow = {
  location: { hash: '#/knowledge' },
  history: {
    pushState(_state, _title, url) {
      fakeWindow.location.hash = String(url);
      pushes.push(`window:${url}`);
    },
    replaceState(_state, _title, url) {
      fakeWindow.location.hash = String(url);
      replaces.push(`window:${url}`);
    }
  },
  addEventListener(type, handler) { events.set(type, handler); },
  removeEventListener(type, handler) {
    if (events.get(type) === handler) events.delete(type);
  }
};

const browser = routerModule.createBrowserHistoryAdapter(fakeWindow);
assert.equal(browser.read(), 'knowledge');
browser.push('files');
assert.equal(fakeWindow.location.hash, '#/files');

let restored = null;
const stop = browser.subscribe(route => { restored = route; });
fakeWindow.location.hash = '#/calendar';
events.get('popstate')();
assert.equal(restored, 'calendar');
stop();
assert.equal(events.has('popstate'), false);

console.log('browser-routing: ok');
