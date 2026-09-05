(()=>{'use strict';
const REPO='beaustwrt248-netizen/Buys-Mock';
const API=`https://api.github.com/repos/${REPO}`;
const esc=value=>String(value??'').replace(/[&<>"']/g,char=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[char]));
const prUrl=pr=>pr.html_url||`https://github.com/${REPO}/pull/${pr.number}`;
const isNova=pr=>String(pr?.head?.ref||'').startsWith('nova/');
const outcome=pr=>pr.merged_at?{label:'MERGED',tone:'ok',date:pr.merged_at}:pr.state==='closed'?{label:'CLOSED',tone:'',date:pr.closed_at||pr.updated_at}:{label:'OPEN',tone:'',date:pr.updated_at};
const item=(pr,status)=>`<a class="item" href="${esc(prUrl(pr))}" target="_blank" rel="noopener noreferrer"><div><b>#${Number(pr.number)} ${esc(pr.title||'Untitled Nova change')}</b><small>${esc(String(pr.head?.ref||'nova/unknown'))} • ${new Date(status.date||0).toLocaleString()}</small></div><span class="tag ${status.tone}">${status.label}</span></a>`;
async function load(){const target=document.getElementById('developmentHistoryList');const state=document.getElementById('developmentHistoryState');if(!target||!state)return;state.textContent='LOADING';state.className='tag';try{const response=await fetch(`${API}/pulls?state=closed&sort=updated&direction=desc&per_page=50`,{headers:{Accept:'application/vnd.github+json'},cache:'no-store'});if(!response.ok)throw new Error(`GitHub ${response.status}`);const prs=(await response.json()).filter(isNova).slice(0,12);target.innerHTML=prs.length?prs.map(pr=>item(pr,outcome(pr))).join(''):'<div class="empty">No completed Nova development work found.</div>';state.textContent=prs.length?`${prs.length} RECENT`:'EMPTY';state.className=`tag ${prs.length?'ok':''}`;}catch(error){console.error(error);target.innerHTML='<div class="empty">Development history is temporarily unavailable. No protected action was attempted.</div>';state.textContent='UNAVAILABLE';state.className='tag high';}}
window.addEventListener('nova:refresh',load);
load();
})();
