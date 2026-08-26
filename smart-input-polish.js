(()=>{
  const NUMERIC_IDS=['mswDealAsk','mswDealMarket','mswDealMax','lapAsk','deskAsk'];
  function polish(root=document){
    for(const id of NUMERIC_IDS){
      const el=root.querySelector?.('#'+id)||document.getElementById(id);
      if(!el||el.dataset.morleyNumericPolished==='1') continue;
      el.dataset.morleyNumericPolished='1';
      el.setAttribute('inputmode','decimal');
      el.setAttribute('min','0');
      el.setAttribute('step','0.01');
      if(el.tagName==='INPUT') el.setAttribute('type','number');
      el.addEventListener('input',()=>{
        const n=Number(el.value);
        if(el.value!==''&&Number.isFinite(n)&&n<0) el.value='0';
      });
    }
  }
  function boot(){
    polish();
    new MutationObserver(records=>{
      for(const record of records){
        for(const node of record.addedNodes){
          if(node.nodeType===1) polish(node);
        }
      }
    }).observe(document.body,{childList:true,subtree:true});
  }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();