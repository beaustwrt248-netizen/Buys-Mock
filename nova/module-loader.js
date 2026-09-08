(()=>{'use strict';
function identity(src){return String(src||'').split('?')[0].replace(/^\.\//,'')}
function matches(script,moduleSrc,id){if(id&&script.id===id)return true;if(script.dataset.novaModule===moduleSrc||script.dataset.novaFeature===moduleSrc)return true;try{return new URL(script.src,location.href).pathname.endsWith('/'+moduleSrc)}catch(_e){return false}}
function find(src,id=''){const moduleSrc=identity(src);if(!moduleSrc)return null;return [...document.scripts].find(script=>matches(script,moduleSrc,id))||null}
function present(src,id=''){return !!find(src,id)}
function load(src,options={}){const moduleSrc=identity(src),id=String(options.id||'');if(!moduleSrc)throw new Error('Nova module source is required.');const existing=find(src,id);if(existing)return existing;const script=document.createElement('script');if(id)script.id=id;script.src=String(src);if(options.ordered===true)script.async=false;else if(options.defer!==false)script.defer=true;script.dataset.novaModule=moduleSrc;if(options.feature===true)script.dataset.novaFeature=moduleSrc;(options.target||document.head).appendChild(script);return script}
window.NovaModuleLoader=Object.freeze({identity,find,present,load});
try{window.dispatchEvent(new CustomEvent('nova:module-loader-ready',{detail:window.NovaModuleLoader}))}catch(_e){}
})();