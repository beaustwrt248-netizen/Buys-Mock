(()=>{'use strict';
const context=window.__morleyAdminAuthContext;
const allowed=['admin','manager'].includes(context?.profile?.role||'');
const q=id=>document.getElementById(id);
const escapeHtml=value=>String(value??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
let rows=[];
function render(){
  const list=q('catalogueList'),status=q('catalogueStatus');if(!list)return;
  const search=String(q('catalogueSearch')?.value||'').trim().toLowerCase();
  const category=String(q('catalogueCategory')?.value||'all');
  const filtered=rows.filter(item=>{
    const hay=[item.category,item.brand,item.model_name,item.model_number,item.storage].join(' ').toLowerCase();
    return(category==='all'||String(item.category||'')===category)&&(!search||hay.includes(search));
  });
  if(status)status.textContent=`Showing ${filtered.length} of ${rows.length} active catalogue item${rows.length===1?'':'s'}. Read-only.`;
  list.innerHTML=filtered.map(item=>`<div class="row"><div class="row-main"><div class="row-title">${escapeHtml([item.brand,item.model_name].filter(Boolean).join(' ')||item.id)}</div><div class="muted">${escapeHtml(item.category||'uncategorised')}${item.model_number?` • ${escapeHtml(item.model_number)}`:''}${item.storage?` • ${escapeHtml(item.storage)}`:''}</div></div><span class="pill ok">ACTIVE</span></div>`).join('')||'<div class="muted">No matching active catalogue items.</div>';
}
async function refresh(){
  const status=q('catalogueStatus');if(!allowed||!window.sb){if(status)status.textContent='Admin or Manager access required.';return;}
  if(status)status.textContent='Loading active catalogue…';
  const {data,error}=await sb.from('device_catalog').select('id,category,brand,model_name,model_number,storage,active').eq('active',true).order('category',{ascending:true}).order('brand',{ascending:true}).order('model_name',{ascending:true}).limit(250);
  if(error){if(status)status.textContent=error.message;return;}
  rows=data||[];
  const category=q('catalogueCategory');if(category){const current=category.value;const values=[...new Set(rows.map(item=>String(item.category||'')).filter(Boolean))].sort();category.innerHTML='<option value="all">All categories</option>'+values.map(value=>`<option value="${escapeHtml(value)}">${escapeHtml(value)}</option>`).join('');if(values.includes(current))category.value=current;}
  render();
}
function bind(){q('catalogueRefreshBtn')?.addEventListener('click',refresh);q('catalogueSearch')?.addEventListener('input',render);q('catalogueCategory')?.addEventListener('change',render);document.querySelectorAll('[data-workspace="catalogue"],[data-open-workspace="catalogue"]').forEach(node=>node.addEventListener('click',()=>{if(!rows.length)refresh();}));}
window.MorleyAdminCatalogueParity=Object.freeze({refresh,render});if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',bind,{once:true});else bind();
})();
