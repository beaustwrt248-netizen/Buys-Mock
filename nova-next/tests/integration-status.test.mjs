import test from 'node:test';
import assert from 'node:assert/strict';
import { normaliseIntegration } from '../src/integration-status.mjs';

test('normalises verified integration capabilities without inventing connections',()=>{
  assert.deepEqual(normaliseIntegration({id:'github_broker',state:'connected',mode:'status-only',detail:'Configured'}),{
    id:'github_broker',label:'GitHub broker',state:'connected',mode:'read-only',capability:'Status only',detail:'Configured'
  });
  assert.equal(normaliseIntegration({id:'x',state:'mystery'}).state,'unavailable');
});

test('protected integration state remains protected',()=>{
  const x=normaliseIntegration({id:'release',state:'protected',mode:'protected',detail:'Approval required'});
  assert.equal(x.state,'protected'); assert.equal(x.mode,'protected');
});
