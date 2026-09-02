(()=>{
  const $=id=>document.getElementById(id);
  async function load(){
    if(!window.sb||!$('guardianAiEnabled'))return;
    const {data,error}=await window.sb.from('guardian_settings').select('ai_enabled,repository_read_enabled,external_research_enabled,agent_model').eq('singleton',true).single();
    if(error){$('guardianAgentStatus').textContent=error.message;return;}
    $('guardianAiEnabled').checked=data?.ai_enabled!==false;
    $('guardianRepoRead').checked=data?.repository_read_enabled!==false;
    $('guardianExternalResearch').checked=!!data?.external_research_enabled;
    $('guardianAgentModel').value=data?.agent_model||'gpt-5.6-terra';
    $('guardianAgentStatus').textContent=`AI ${data?.ai_enabled!==false?'enabled':'disabled'} • repository read ${data?.repository_read_enabled!==false?'enabled':'disabled'} • external research ${data?.external_research_enabled?'enabled':'disabled'}`;
  }
  async function save(){
    if(!window.sb)return;
    const status=$('guardianAgentStatus');status.textContent='Saving Guardian intelligence controls…';
    const {error}=await window.sb.rpc('guardian_set_agent_controls',{
      p_ai_enabled:$('guardianAiEnabled').checked,
      p_repository_read_enabled:$('guardianRepoRead').checked,
      p_external_research_enabled:$('guardianExternalResearch').checked,
      p_agent_model:$('guardianAgentModel').value
    });
    if(error){status.textContent=error.message;return;}
    status.textContent='Guardian intelligence controls saved and audited.';
    await load();
  }
  $('guardianAgentSaveBtn')?.addEventListener('click',save);
  window.addEventListener('load',()=>setTimeout(load,0));
  window.sb?.auth?.onAuthStateChange?.((event,session)=>{if(session)setTimeout(load,0)});
})();
