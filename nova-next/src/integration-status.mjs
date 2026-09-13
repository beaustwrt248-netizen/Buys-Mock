const STATES=new Set(['connected','disconnected','unavailable','protected','staged']);
const MODE_MAP=Object.freeze({'status-only':'read-only','read-only':'read-only','interactive':'interactive','local-only':'local-only','protected':'protected','staged':'protected'});
const LABELS=Object.freeze({nova_session:'Nova Admin session',github_broker:'GitHub broker',release:'Release controls'});
export function normaliseIntegration(item={}){
  const id=String(item.id||'unknown');
  const state=STATES.has(item.state)?item.state:'unavailable';
  const mode=MODE_MAP[item.mode] || (state==='protected'?'protected':'read-only');
  const capability=mode==='read-only'?'Status only':mode==='local-only'?'Local only':mode==='interactive'?'Safe actions':'Protected';
  return Object.freeze({id,label:LABELS[id]||String(item.label||id).replaceAll('_',' '),state,mode,capability,detail:String(item.detail||'No verified status available.')});
}
