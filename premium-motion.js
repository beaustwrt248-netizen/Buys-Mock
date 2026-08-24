(()=>{
 const root=document.documentElement;
 let raf=0;
 function pointer(e){if(innerWidth<900)return;cancelAnimationFrame(raf);raf=requestAnimationFrame(()=>{root.style.setProperty('--mx',`${e.clientX}px`);root.style.setProperty('--my',`${e.clientY}px`)})}
 addEventListener('pointermove',pointer,{passive:true});
 function enhance(){
   document.querySelectorAll('button').forEach(b=>{if(b.dataset.morleyFx)return;b.dataset.morleyFx='1';b.addEventListener('pointerdown',e=>{if(matchMedia('(prefers-reduced-motion: reduce)').matches)return;const r=b.getBoundingClientRect(),x=e.clientX-r.left,y=e.clientY-r.top,s=document.createElement('i');s.style.cssText=`position:absolute;left:${x}px;top:${y}px;width:8px;height:8px;border-radius:50%;pointer-events:none;background:rgba(255,255,255,.38);transform:translate(-50%,-50%) scale(0);animation:morleyRipple .48s ease-out forwards;z-index:5`;b.appendChild(s);setTimeout(()=>s.remove(),520)})});
 }
 const style=document.createElement('style');style.textContent='@keyframes morleyRipple{to{transform:translate(-50%,-50%) scale(18);opacity:0}}';document.head.appendChild(style);
 enhance();new MutationObserver(enhance).observe(document.body,{childList:true,subtree:true});
})();