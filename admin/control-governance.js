(function(){
  function governFeatureControls(){
    const container=document.getElementById('featureFlags');
    if(!container||typeof config!=='object')return;
    const flags=config.feature_flags||{};
    const rows=[
      ['deviceScanner','Device scanner',false],
      ['googlePricing','Google pricing',false],
      ['ebayPricing','eBay pricing',false],
      ['valuationHistory','Valuation history',false],
      ['maintenanceMode','Maintenance mode',true]
    ];
    container.innerHTML=rows.map(([key,label,writable])=>`<label class="switchrow"><span>${esc(label)}${writable?'':' <small class="muted">(read-only)</small>'}</span><input type="checkbox" data-flag="${key}" ${flags[key]?'checked':''} ${writable?'':'disabled'}></label>`).join('');
  }

  const previousLoadConfig=loadConfig;
  loadConfig=async function(){
    await previousLoadConfig();
    governFeatureControls();
  };

  const save=document.getElementById('saveControlsBtn');
  if(save){
    save.onclick=async()=>{
      const flags={...(config.feature_flags||{})};
      const maintenance=document.querySelector('[data-flag="maintenanceMode"]');
      flags.maintenanceMode=!!maintenance?.checked;
      flags.maintenanceMessage=document.getElementById('maintenanceMessage').value.trim().slice(0,160);
      if(typeof flags.otaEnabled!=='boolean')flags.otaEnabled=true;
      const status=document.getElementById('controlsStatus');
      status.textContent='Saving audited maintenance control…';
      save.disabled=true;
      const {error}=await sb.rpc('admin_set_config',{config_key:'feature_flags',config_value:flags});
      status.textContent=error?error.message:'Maintenance control saved.';
      save.disabled=false;
      if(!error){config.feature_flags=flags;await Promise.all([loadConfig(),loadAudit()]);}
    };
  }

  governFeatureControls();
})();
