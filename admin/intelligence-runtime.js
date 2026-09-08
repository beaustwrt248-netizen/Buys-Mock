(function(root,factory){const api=factory(root.MorleyIntelligenceEngine);if(typeof module==='object'&&module.exports)module.exports=api;root.MorleyIntelligenceRuntime=api;})(typeof globalThis!=='undefined'?globalThis:this,function(engine){'use strict';
const state={catalogue:[],pricing:[],marketplace:[],system:[],updatedAt:null,sources:{}};const listeners=new Set();
function clone(v){return JSON.parse(JSON.stringify(v))}
function publish(){state.updatedAt=new Date().toISOString();const snap=snapshot();for(const fn of listeners)try{fn(snap)}catch{}if(typeof window!=='undefined'&&window.dispatchEvent)window.dispatchEvent(new CustomEvent('morley:intelligence-updated',{detail:snap}));return snap}
function ingest(kind,rows,meta={}){if(!Object.prototype.hasOwnProperty.call(state,kind)||kind==='sources'||kind==='updatedAt')throw new Error('Unsupported intelligence dataset');state[kind]=Array.isArray(rows)?clone(rows):[];state.sources[kind]={source:typeof meta.source==='string'?meta.source:null,checkedAt:typeof meta.checkedAt==='string'?meta.checkedAt:new Date().toISOString(),authoritative:meta.authoritative===true};return publish()}
function catalogue(){if(!engine)return {state:'unavailable',devices:[],findings:[]};return {state:state.catalogue.length?'confirmed':'unavailable',devices:clone(state.catalogue),findings:engine.catalogueFindings(state.catalogue),source:clone(state.sources.catalogue||{})}}
function pricing(options={}){if(!engine)return {state:'unavailable'};return engine.pricingRecommendation(state.pricing,options)}
function marketplace(){if(!engine)return [];const input=state.marketplace.find(x=>x&&Array.isArray(x.stock)&&Array.isArray(x.listings));return input?engine.marketplaceMatch(input.stock,input.listings):[]}
function alerts(){if(!engine)return [];return engine.smartAlerts({catalogueFindings:catalogue().findings,marketplace:marketplace(),system:state.system})}
function snapshot(){return Object.freeze({updatedAt:state.updatedAt,sources:clone(state.sources),catalogue:catalogue(),pricing:pricing(),pricingObservations:clone(state.pricing),systemEvidence:clone(state.system),marketplace:marketplace(),alerts:alerts()})}
function subscribe(fn){if(typeof fn!=='function')return()=>{};listeners.add(fn);return()=>listeners.delete(fn)}
function clear(kind){if(kind&&Object.prototype.hasOwnProperty.call(state,kind)&&!['sources','updatedAt'].includes(kind)){state[kind]=[];delete state.sources[kind];return publish()}for(const k of ['catalogue','pricing','marketplace','system'])state[k]=[];state.sources={};return publish()}
return Object.freeze({ingest,snapshot,subscribe,clear});
});
