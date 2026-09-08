(()=>{
'use strict';

const PRODUCTS=Object.freeze({
  buys:Object.freeze({
    id:'morley-buys',
    name:'Morley Buys',
    purpose:'operations',
    owns:Object.freeze(['valuations','trade-ins','device-lookup','staff-workflows','operational-pricing-view']),
    protected:false
  }),
  admin:Object.freeze({
    id:'morley-admin',
    name:'Morley Admin',
    purpose:'control-centre',
    owns:Object.freeze(['accounts','roles','catalogue-administration','pricing-controls','integrations','backups','audit','release-controls','system-health']),
    protected:true
  }),
  nova:Object.freeze({
    id:'nova',
    name:'Nova by Morley',
    purpose:'intelligence-automation',
    owns:Object.freeze(['research','catalogue-intelligence','pricing-intelligence','image-intelligence','support-intelligence','monitoring','recommendations','automation']),
    protected:true
  })
});

const CORE=Object.freeze({
  name:'Morley Core',
  sourceOfTruth:Object.freeze(['catalogue','pricing','identity','roles','media','audit-events','search','integrations','notifications','realtime-events']),
  rule:'Products consume canonical Morley Core data; they do not create competing sources of truth.'
});

const GUARDIAN=Object.freeze({
  id:'guardian',
  name:'Guardian',
  product:false,
  parent:'nova',
  purpose:'protected security, governance and enforcement layer',
  compatibility:Object.freeze({
    preserveDatabasePrefix:'guardian_',
    preserveAuditHistory:true,
    preserveLegacyApiNames:true
  }),
  invariants:Object.freeze([
    'Nova cannot disable, bypass or weaken Guardian.',
    'Nova cannot self-approve protected work.',
    'Destructive, release, auth, permission and protected pricing actions remain human-gated.',
    'Missing enforcement evidence fails closed.'
  ])
});

const contract=Object.freeze({version:1,products:PRODUCTS,core:CORE,guardian:GUARDIAN});
window.MorleyEcosystem=contract;
try{window.dispatchEvent(new CustomEvent('morley:ecosystem-ready',{detail:contract}))}catch(_){/* older WebViews */}
})();
