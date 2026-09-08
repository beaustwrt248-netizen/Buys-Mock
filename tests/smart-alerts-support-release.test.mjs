import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
globalThis.MorleyIntelligence={};
const engine=require('../admin/intelligence-engine.js');

const alerts=engine.smartAlerts({
 support:[{id:'ticket-1',subject:'Cannot value device',priority:'urgent',status:'open',slaRisk:false},{id:'ticket-2',subject:'SLA due',priority:'normal',status:'open',slaRisk:true}],
 release:[{id:'ota-1',versionName:'2.0.0',versionCode:20,sourceVersionCode:21,manifestVerified:true,checksumVerified:true,rollout:{current:6,outdated:4,ahead:0,unknown:0}}]
});
assert.equal(alerts.filter(x=>x.type==='support').length,2,'urgent and SLA-risk tickets become support alerts');
assert.ok(alerts.some(x=>x.key==='release:drift:ota-1'&&x.severity==='high'),'source/OTA drift becomes a high release alert');
assert.ok(alerts.some(x=>x.key==='release:rollout:ota-1'&&x.severity==='medium'),'25%+ outdated rollout becomes a release alert');
assert.ok(!alerts.some(x=>x.key==='release:checksum:ota-1'),'verified checksum does not create a blocker');
const blocked=engine.smartAlerts({release:[{id:'ota-2',manifestVerified:false,checksumVerified:false,rollout:{}}]});
assert.equal(blocked.filter(x=>x.severity==='critical').length,2,'unverified manifest and checksum fail closed');
console.log('smart-alerts support/release contract: ok');
