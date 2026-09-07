import fs from 'node:fs';
import assert from 'node:assert/strict';

const html=fs.readFileSync(new URL('../nova/index.html',import.meta.url),'utf8');
const app=fs.readFileSync(new URL('../nova/app.js',import.meta.url),'utf8');
const core=fs.readFileSync(new URL('../nova/app-core.js',import.meta.url),'utf8');
const css=fs.readFileSync(new URL('../nova/styles.css',import.meta.url),'utf8');

for(const label of ['Overview','Needs Attention','Development','Guardian','Catalogue','Support','Knowledge','Monitoring','Releases','Recommendations','Activity']) assert.ok(html.includes(label),`missing ${label}`);
assert.ok(html.includes('© 2026 Morley Buys'));
assert.ok(html.includes('Guardian protected'));
assert.ok(html.includes('Human gated'));
assert.ok(html.includes('aria-label="Nova sections"'));
assert.ok(app.includes("script.src='app-core.js?v=4'"));
for(const cap of ['renderCatalogue','renderSupport','renderMonitoring','renderKnowledge']) assert.ok(core.includes(`function ${cap}`),`missing ${cap}`);
assert.ok(core.includes("No protected action was attempted"));
assert.ok(css.includes('@media(max-width:820px)'));
assert.ok(css.includes('@media(max-width:560px)'));
for(const forbidden of ['SUPABASE_SERVICE_ROLE_KEY','service_role','auth.admin']) assert.ok(![html,app,core,css].join('\n').includes(forbidden),`forbidden client authority token: ${forbidden}`);
console.log('Nova standalone control centre contract passed');
