(()=>{
function isOEMRow(btn){
  const part=btn?.closest?.('.part');
  const title=part?.querySelector?.('b')?.textContent||'';
  return /Whole OEM System/i.test(title);
}

document.addEventListener('click',e=>{
  const btn=e.target?.closest?.('.psearch');
  if(!btn||!isOEMRow(btn))return;
  e.preventDefault();
  e.stopPropagation();
  e.stopImmediatePropagation();
  const analyse=document.getElementById('deskAnalyse');
  if(analyse&&!analyse.disabled) analyse.click();
},true);
})();