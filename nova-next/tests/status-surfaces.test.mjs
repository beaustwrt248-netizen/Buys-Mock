import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createFeatureRuntime } from '../src/feature-runtime.mjs';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const ui = await readFile(new URL('../src/workspace-ui.mjs', import.meta.url), 'utf8');
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');

function edgeClient({ github = { ok: true, configured: true } } = {}) {
  return {
    async invoke(name, body) {
      if (name === 'nova-github') {
        if (github instanceof Error) throw github;
        return github;
      }
      return { ok: true, name, body };
    }
  };
}

test('automation status is derived from existing safe capabilities and keeps protected actions staged', () => {
  const runtime = createFeatureRuntime({ getAccessToken: () => 'token', edgeClient: edgeClient() });
  const status = runtime.automationStatus();
  const byId = Object.fromEntries(status.map(item => [item.id, item]));

  assert.equal(byId.guarded_chat.state, 'available');
  assert.equal(byId.vision.state, 'available');
  assert.equal(byId.knowledge.mode, 'read-only');
  assert.equal(byId.github_broker.mode, 'status-only');
  assert.equal(byId.code_proposal.mode, 'proposal-only');
  assert.equal(byId.scheduling.state, 'staged');
  assert.equal(byId.guardian_repair.state, 'protected');
  assert.equal(byId.release_ota.state, 'protected');
  assert.equal(byId.pricing_write.state, 'protected');
  assert.equal(status.some(item => item.mode === 'executor'), false);
});

test('integration status reports authenticated Nova session and read-only GitHub broker truthfully', async () => {
  const runtime = createFeatureRuntime({ getAccessToken: () => 'token', edgeClient: edgeClient() });
  const status = await runtime.integrationStatus();
  const byId = Object.fromEntries(status.map(item => [item.id, item]));

  assert.equal(byId.nova_session.state, 'connected');
  assert.equal(byId.nova_session.readOnly, false);
  assert.equal(byId.github_broker.state, 'connected');
  assert.equal(byId.github_broker.readOnly, true);
});

test('integration status preserves partial failure and never fabricates connectivity', async () => {
  const runtime = createFeatureRuntime({ getAccessToken: () => 'token', edgeClient: edgeClient({ github: new Error('offline') }) });
  const status = await runtime.integrationStatus();
  const byId = Object.fromEntries(status.map(item => [item.id, item]));

  assert.equal(byId.nova_session.state, 'connected');
  assert.equal(byId.github_broker.state, 'unavailable');
  assert.match(byId.github_broker.detail, /offline/i);

  const locked = createFeatureRuntime({ getAccessToken: () => '', edgeClient: edgeClient() });
  const lockedStatus = await locked.integrationStatus();
  assert.equal(lockedStatus.find(item => item.id === 'nova_session').state, 'disconnected');
});

test('Automation and Integrations use runtime-owned containers with no credential entry surface', () => {
  assert.ok(html.includes('id="novaNextAutomationList"'));
  assert.ok(html.includes('id="novaNextIntegrationsList"'));
  assert.ok(html.includes('No scheduling, release, Guardian repair or pricing authority is granted here.'));
  assert.ok(html.includes('No credentials are entered or stored on this screen.'));
  assert.equal(html.includes('data-route="integrations"><div class="placeholder"'), false);
  assert.equal(html.includes('data-route="automation"><div class="placeholder"'), false);
});

test('workspace UI renders Automation and Integrations and handles route refreshes', () => {
  for (const token of ['renderAutomation', 'renderIntegrations', "route === 'automation'", "route === 'integrations'", 'featureRuntime.automationStatus()', 'featureRuntime.integrationStatus()']) {
    assert.ok(ui.includes(token), token);
  }
  assert.ok(ui.includes("state === 'connected'"));
  assert.ok(ui.includes("state === 'unavailable'"));
});

test('Settings and route subtitles describe status-only surfaces truthfully', () => {
  assert.ok(html.includes('<strong>Integrations</strong><small>Verified connection status</small>'));
  assert.ok(html.includes('<strong>Automation</strong><small>Capability status &amp; boundaries</small>'));
  assert.ok(app.includes("automation: 'Capability status'"));
  assert.ok(app.includes("integrations: 'Verified connections'"));
  assert.equal(html.includes('<strong>Integrations</strong><small>Connect your tools</small>'), false);
  assert.equal(html.includes('<strong>Automation</strong><small>Schedules and workflows</small>'), false);
});
