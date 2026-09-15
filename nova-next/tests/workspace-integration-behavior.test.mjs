import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorkspaceUi } from '../src/workspace-ui.mjs';
import { createFeatureRuntime } from '../src/feature-runtime.mjs';

class Element {
  constructor(tag) { this.tagName = tag; this.children = []; this.listeners = {}; this.textContent = ''; }
  get firstChild() { return this.children[0]; }
  append(...nodes) { for (const node of nodes) { node.parent = this; this.children.push(node); } }
  remove() { this.parent.children.splice(this.parent.children.indexOf(this), 1); }
  setAttribute() {}
  addEventListener(type, listener) { this.listeners[type] = listener; }
  click() { this.listeners.click?.(); }
}

function deferred() {
  let resolve, reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

function setup(t, integrationStatus) {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const diagnostics = t.mock.method(console, 'error', () => {});
  const list = new Element('div');
  const ui = createWorkspaceUi({
    workspaceRuntime: {}, featureRuntime: { integrationStatus },
    documentObj: {
      createElement: tag => new Element(tag),
      getElementById: id => id === 'novaNextIntegrationsList' ? list : null
    }
  });
  return { ui, list, diagnostics };
}

const text = node => [node.textContent, ...node.children.map(text)].join(' ');
const buttons = node => node.children.flatMap(child => child.tagName === 'button' ? [child] : buttons(child));
const status = label => [{ id: 'test', label, state: 'connected', readOnly: true, detail: 'Verified read-only connection.' }];
const settle = async () => { for (let i = 0; i < 8; i++) await Promise.resolve(); };

test('hung integration request escapes loading and Retry integrations makes a fresh request', async t => {
  const first = deferred();
  let requests = 0;
  const { ui, list, diagnostics } = setup(t, () => ++requests === 1 ? first.promise : Promise.resolve(status('Recovered')));
  const pending = ui.renderIntegrations();
  assert.match(text(list), /Checking verified connection status/);
  t.mock.timers.tick(5999);
  await settle();
  assert.match(text(list), /Checking verified connection status/);
  t.mock.timers.tick(1);
  await settle();
  assert.doesNotMatch(text(list), /Checking verified connection status/);
  assert.match(text(list), /could not be verified/);
  assert.doesNotMatch(text(list), /WORKSPACE_ROUTE_TIMEOUT/);
  const retry = buttons(list).find(button => button.textContent === 'Retry integrations');
  assert.ok(retry);
  retry.click();
  await settle();
  assert.equal(requests, 2);
  assert.match(text(list), /Recovered/);
  first.resolve(status('Obsolete'));
  await pending;
  await settle();
  assert.doesNotMatch(text(list), /Obsolete/);
  assert.equal(diagnostics.mock.calls.length, 1);
});

for (const sync of [false, true]) {
  test(`integration ${sync ? 'synchronous throw' : 'rejection'} offers retry without leaking diagnostics`, async t => {
    const error = new Error('private-provider-diagnostic');
    const { ui, list, diagnostics } = setup(t, () => { if (sync) throw error; return Promise.reject(error); });
    await ui.renderIntegrations();
    assert.doesNotMatch(text(list), /private-provider-diagnostic|Checking verified/);
    assert.ok(buttons(list).some(button => button.textContent === 'Retry integrations'));
    assert.equal(diagnostics.mock.calls[0].arguments[1], error);
  });
}

for (const reject of [false, true]) {
  for (const navigate of [false, true]) {
    test(`stale ${reject ? 'failure' : 'success'} cannot overwrite ${navigate ? 'a route transition' : 'a newer render'}`, async t => {
      const old = deferred();
      let requests = 0;
      const { ui, list } = setup(t, () => ++requests === 1 ? old.promise : Promise.resolve(status('Current')));
      const pending = ui.renderIntegrations();
      if (navigate) ui.routeChanged('chat');
      else await ui.renderIntegrations();
      const before = text(list);
      if (reject) old.reject(new Error('obsolete-error'));
      else old.resolve(status('Obsolete'));
      await pending;
      assert.equal(text(list), before);
      if (navigate) {
        ui.routeChanged('integrations');
        await settle();
        assert.match(text(list), /Current/);
      }
    });
  }
}

test('successful and empty integration responses remain stable beyond the deadline', async t => {
  let requests = 0;
  const { ui, list, diagnostics } = setup(t, () => Promise.resolve(++requests === 1 ? status('Available') : []));
  await ui.renderIntegrations();
  assert.match(text(list), /Available/);
  t.mock.timers.tick(10000);
  await settle();
  assert.match(text(list), /Available/);
  await ui.renderIntegrations();
  assert.match(text(list), /No identifiable integrations/);
  assert.equal(diagnostics.mock.calls.length, 0);
});

test('broker failure returned by the real runtime is safe and retryable', async t => {
  let requests = 0;
  const runtime = createFeatureRuntime({
    getAccessToken: () => 'test-session',
    edgeClient: { invoke: async () => {
      if (++requests === 1) throw new Error('private-broker-diagnostic');
      return { configured: true };
    } }
  });
  const { ui, list, diagnostics } = setup(t, runtime.integrationStatus);
  await ui.renderIntegrations();
  assert.doesNotMatch(text(list), /private-broker-diagnostic/);
  assert.match(text(list), /could not be verified/);
  const retry = buttons(list).find(button => button.textContent === 'Retry integrations');
  assert.ok(retry);
  retry.click();
  await settle();
  assert.equal(requests, 2);
  assert.match(text(list), /Read-only broker status is configured/);
  assert.ok(diagnostics.mock.calls.length > 0);
});
