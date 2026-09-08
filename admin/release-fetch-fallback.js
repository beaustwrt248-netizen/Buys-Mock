(()=>{'use strict';
const nativeFetch=window.fetch?.bind(window);if(!nativeFetch)return;
const rawOta='https://raw.githubusercontent.com/beaustwrt248-netizen/Buys-Mock/main/ota/latest.json';
const rawBuild='https://raw.githubusercontent.com/beaustwrt248-netizen/Buys-Mock/main/android/app/build.gradle';
function localFor(url){if(!/^https?:$/.test(location.protocol))return null;if(url.startsWith(rawOta))return new URL('../ota/latest.json',location.href).href;if(url.startsWith(rawBuild))return new URL('../android/app/build.gradle',location.href).href;return null}
window.fetch=async function(input,init){const raw=typeof input==='string'?input:input?.url||'';const local=localFor(raw);if(!local)return nativeFetch(input,init);try{const localResponse=await nativeFetch(local,{...init,cache:'no-store'});if(localResponse.ok)return localResponse}catch{}return nativeFetch(input,init)};
function retryRelease(){if(typeof window.refreshVerifiedOtaRelease==='function')window.refreshVerifiedOtaRelease(true).catch(()=>{})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(retryRelease,0),{once:true});else setTimeout(retryRelease,0);
})();
