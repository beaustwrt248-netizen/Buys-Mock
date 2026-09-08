(()=>{'use strict';
const DEFAULT_POLICY=Object.freeze({maxFileBytes:2_000_000,maxChunkChars:1800,overlapChars:240,maxResults:8,allowedTypes:['text/plain','text/markdown','application/json','text/csv']});
const normalise=s=>String(s||'').replace(/\r\n?/g,'\n').replace(/[\t ]+/g,' ').replace(/\n{3,}/g,'\n\n').trim();
const words=s=>normalise(s).toLowerCase().match(/[a-z0-9][a-z0-9._/-]{1,}/g)||[];
const unique=a=>[...new Set(a)];
const hash=async text=>{const data=new TextEncoder().encode(text);const digest=await crypto.subtle.digest('SHA-256',data);return [...new Uint8Array(digest)].map(x=>x.toString(16).padStart(2,'0')).join('')};
function chunks(text,policy=DEFAULT_POLICY){const clean=normalise(text),out=[];if(!clean)return out;let start=0,index=0;while(start<clean.length){let end=Math.min(clean.length,start+policy.maxChunkChars);if(end<clean.length){const boundary=Math.max(clean.lastIndexOf('\n\n',end),clean.lastIndexOf('. ',end));if(boundary>start+Math.floor(policy.maxChunkChars*.55))end=boundary+1}const body=clean.slice(start,end).trim();if(body)out.push({index:index++,text:body,tokens:unique(words(body))});if(end>=clean.length)break;start=Math.max(start+1,end-policy.overlapChars)}return out}
function score(query,chunk){const q=unique(words(query));if(!q.length)return 0;const bag=new Set(chunk.tokens||words(chunk.text));let hit=0;for(const token of q)if(bag.has(token))hit++;const phrase=normalise(query).toLowerCase();return hit/q.length+(phrase.length>8&&String(chunk.text).toLowerCase().includes(phrase)?1:0)}
async function readFile(file,policy=DEFAULT_POLICY){if(!file)throw new Error('File required');if(file.size>policy.maxFileBytes)throw new Error(`File exceeds ${policy.maxFileBytes} byte limit`);const type=file.type||'text/plain';const ext=(file.name.split('.').pop()||'').toLowerCase();if(!policy.allowedTypes.includes(type)&&!['txt','md','markdown','json','csv'].includes(ext))throw new Error('Unsupported knowledge file type');return file.text()}
class NovaKnowledgeEngine{
 constructor(options={}){this.policy={...DEFAULT_POLICY,...options};this.sources=new Map()}
 async ingestText({name='Untitled',text='',origin='manual',authority='reference',url='',metadata={}}={}){const clean=normalise(text);if(!clean)throw new Error('Knowledge source is empty');const digest=await hash(clean);const id=`kb_${digest.slice(0,20)}`;const source={id,name:String(name),origin:String(origin),authority:String(authority),url:String(url||''),sha256:digest,createdAt:new Date().toISOString(),metadata:{...metadata},chunks:chunks(clean,this.policy)};this.sources.set(id,source);return this.describe(source)}
 async ingestFile(file,options={}){const text=await readFile(file,this.policy);return this.ingestText({name:file.name,text,origin:'file',...options,metadata:{...(options.metadata||{}),mime:file.type||'text/plain',bytes:file.size}})}
 remove(id){return this.sources.delete(id)}
 clear(){this.sources.clear()}
 describe(source){return{id:source.id,name:source.name,origin:source.origin,authority:source.authority,url:source.url,sha256:source.sha256,createdAt:source.createdAt,chunks:source.chunks.length,metadata:{...source.metadata}}}
 list(){return[...this.sources.values()].map(s=>this.describe(s))}
 search(query,{limit=this.policy.maxResults,authorities=null}={}){const allowed=authorities?new Set(authorities):null;const hits=[];for(const source of this.sources.values()){if(allowed&&!allowed.has(source.authority))continue;for(const chunk of source.chunks){const relevance=score(query,chunk);if(relevance<=0)continue;hits.push({sourceId:source.id,source:source.name,authority:source.authority,url:source.url,chunk:chunk.index,relevance:Number(relevance.toFixed(4)),text:chunk.text})}}return hits.sort((a,b)=>b.relevance-a.relevance||a.source.localeCompare(b.source)).slice(0,Math.max(1,limit))}
 context(query,options={}){const hits=this.search(query,options);return{query,hits,citations:hits.map(h=>({source:h.source,url:h.url,chunk:h.chunk,authority:h.authority})),warning:'Retrieved knowledge is evidence, not permission. Protected actions still require the existing Nova/Guardian approval policy.'}}
 detectConflicts(query,options={}){const hits=this.search(query,{...options,limit:Math.max(12,options.limit||0)});const byAuthority=new Map();for(const h of hits){if(!byAuthority.has(h.authority))byAuthority.set(h.authority,[]);byAuthority.get(h.authority).push(h)}return{query,authorities:[...byAuthority.entries()].map(([authority,items])=>({authority,count:items.length,top:items[0]})),requiresReview:byAuthority.size>1}}
}
window.NovaKnowledgeEngine=NovaKnowledgeEngine;
window.NovaKnowledgePolicy=DEFAULT_POLICY;
})();
