(()=>{'use strict';
function loadContract(){if(window.MorleyEcosystem||document.querySelector('#morleyEcosystemContract'))return;const s=document.createElement('script');s.id='morleyEcosystemContract';s.src='../morley-core.js?v=1';s.defer=true;document.head.appendChild(s)}
function apply(){const link=[...document.querySelectorAll('a[href="guardian.html"]')][0];if(link){link.textContent='Nova Security';link.setAttribute('aria-label','Nova Security and Guardian enforcement');link.title='Nova Security · Guardian Enforcement'}document.documentElement.dataset.morleyEcosystem='admin'}
function start(){loadContract();apply();new MutationObserver(apply).observe(document.body,{subtree:true,childList:true})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start,{once:true});else start();
})();