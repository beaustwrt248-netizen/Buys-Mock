const OTA_METADATA_URL='https://raw.githubusercontent.com/beaustwrt248-netizen/Buys-Mock/main/ota/latest.json';
const OTA_RELEASE_PREFIX='https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/';
let verifiedOtaRelease=null;
let otaLoadPromise=null;

function isVerifiedOtaShape(value){
  return !!value&&Number.isInteger(Number(value.versionCode))&&Number(value.versionCode)>0&&String(value.versionName||'').trim().length>0&&String(value.apkUrl||'').startsWith(OTA_RELEASE_PREFIX)&&/^[a-fA-F0-9]{64}$/.test(String(value.sha256||''));
}

async function fetchVerifiedOtaRelease(){
  const response=await fetch(`${OTA_METADATA_URL}?t=${Date.now()}`,{cache:'no-store',headers:{'Cache-Control':'no-cache'}});
  if(!response.ok)throw new Error(`OTA manifest returned HTTP ${response.status}`);
  const data=await response.json();
  if(!isVerifiedOtaShape(data))throw new Error('OTA manifest failed release validation');
  return {
    versionCode:Number(data.versionCode),
    versionName:String(data.versionName).trim(),
    apkUrl:String(data.apkUrl).trim(),
    sha256:String(data.sha256).trim().toLowerCase(),
    notes:String(data.notes||'').trim()
  };
}

function releaseConfigMatchesOta(){
  const saved=config?.current_release||{};
  return Number(saved.versionCode||0)===verifiedOtaRelease?.versionCode&&String(saved.versionName||'')===verifiedOtaRelease?.versionName&&String(saved.apkUrl||'')===verifiedOtaRelease?.apkUrl&&String(saved.sha256||'').toLowerCase()===verifiedOtaRelease?.sha256;
}

function versionParts(value){
  const match=String(value||'').trim().match(/^(\d+)\.(\d+)\.(\d+)(?:[-+].*)?$/);
  return match?match.slice(1).map(Number):null;
}

function compareVersions(left,right){
  const a=versionParts(left),b=versionParts(right);
  if(!a||!b)return null;
  for(let i=0;i<3;i++){if(a[i]!==b[i])return a[i]>b[i]?1:-1}
  return 0;
}

async function loadReleaseRollout(){
  if(!verifiedOtaRelease)return;
  const {data,error}=await sb.from('devices').select('app_version,last_seen_at');
  if(error){$('rolloutStatus').textContent=`Rollout health unavailable: ${error.message}`;return}
  let current=0,outdated=0,ahead=0,unknown=0;
  let latestSeen=0;
  (data||[]).forEach(device=>{
    const version=String(device.app_version||'').trim();
    const seen=Date.parse(device.last_seen_at||'')||0;
    latestSeen=Math.max(latestSeen,seen);
    if(!version){unknown++;return}
    if(version===verifiedOtaRelease.versionName){current++;return}
    const comparison=compareVersions(version,verifiedOtaRelease.versionName);
    if(comparison===null){unknown++}
    else if(comparison<0){outdated++}
    else if(comparison>0){ahead++}
    else{current++}
  });
  $('rolloutCurrent').textContent=current;
  $('rolloutOutdated').textContent=outdated;
  $('rolloutAhead').textContent=ahead;
  $('rolloutUnknown').textContent=unknown;
  const total=(data||[]).length;
  const seenText=latestSeen?` Latest device contact ${new Date(latestSeen).toLocaleString()}.`:'';
  $('rolloutStatus').textContent=`${total} registered device${total===1?'':'s'} checked against OTA ${verifiedOtaRelease.versionName}.${seenText}`;
}

function renderVerifiedOtaRelease(){
  if(!verifiedOtaRelease)return;
  $('releaseName').value=verifiedOtaRelease.versionName;
  $('releaseCode').value=verifiedOtaRelease.versionCode;
  if($('releaseSha'))$('releaseSha').value=verifiedOtaRelease.sha256;
  if($('releaseUrl'))$('releaseUrl').value=verifiedOtaRelease.apkUrl;
  if($('releaseNotes'))$('releaseNotes').value=verifiedOtaRelease.notes;
  $('metricVersion').textContent=verifiedOtaRelease.versionName;
  $('saveReleaseBtn').disabled=false;
  $('releaseStatus').textContent=releaseConfigMatchesOta()
    ?'Signed OTA metadata verified. Admin release state is in sync.'
    :'Signed OTA metadata verified. Admin release state is out of sync; publishing support policy will safely sync it.';
  loadReleaseRollout().catch(error=>{$('rolloutStatus').textContent=`Rollout health unavailable: ${error.message||'unknown error'}`});
}

async function refreshVerifiedOtaRelease(force=false){
  if(otaLoadPromise&&!force)return otaLoadPromise;
  $('saveReleaseBtn').disabled=true;
  $('releaseStatus').textContent='Verifying signed OTA release metadata…';
  otaLoadPromise=(async()=>{
    try{
      verifiedOtaRelease=await fetchVerifiedOtaRelease();
      renderVerifiedOtaRelease();
      return verifiedOtaRelease;
    }catch(error){
      verifiedOtaRelease=null;
      $('saveReleaseBtn').disabled=true;
      $('releaseStatus').textContent=`Release controls locked: ${error.message||'OTA verification failed'}.`;
      $('rolloutStatus').textContent='Rollout health is locked until OTA metadata verifies.';
      throw error;
    }finally{
      otaLoadPromise=null;
    }
  })();
  return otaLoadPromise;
}

const originalLoadConfig=loadConfig;
loadConfig=async function(){
  await originalLoadConfig();
  try{await refreshVerifiedOtaRelease()}catch{}
};

const originalLoadMetrics=loadMetrics;
loadMetrics=async function(){
  await originalLoadMetrics();
  if(verifiedOtaRelease){$('metricVersion').textContent=verifiedOtaRelease.versionName;await loadReleaseRollout()}
  else try{await refreshVerifiedOtaRelease()}catch{}
};

document.querySelector('[data-tab="release"]')?.addEventListener('click',()=>{refreshVerifiedOtaRelease(true).catch(()=>{})});

$('saveReleaseBtn').onclick=async()=>{
  $('releaseStatus').textContent='Re-verifying signed OTA release…';
  let current;
  try{current=await refreshVerifiedOtaRelease(true)}catch{return}
  const minimum={
    versionName:$('minName').value.trim(),
    versionCode:Number($('minCode').value||0),
    forceUpdate:$('forceUpdate').checked
  };
  if(!Number.isInteger(minimum.versionCode)||minimum.versionCode<1){$('releaseStatus').textContent='Minimum supported version code must be a positive whole number.';return}
  if(minimum.versionCode>current.versionCode){$('releaseStatus').textContent=`Minimum supported version cannot exceed the signed OTA release (${current.versionCode}).`;return}
  if(!minimum.versionName){$('releaseStatus').textContent='Enter the minimum supported version name.';return}
  if(minimum.versionCode===current.versionCode)minimum.versionName=current.versionName;
  $('saveReleaseBtn').disabled=true;
  $('releaseStatus').textContent='Publishing verified support policy…';
  const minimumResult=await sb.rpc('admin_set_config',{config_key:'minimum_supported_version',config_value:minimum});
  if(minimumResult.error){$('saveReleaseBtn').disabled=false;$('releaseStatus').textContent=minimumResult.error.message;return}
  const releaseResult=await sb.rpc('admin_set_config',{config_key:'current_release',config_value:current});
  if(releaseResult.error){$('saveReleaseBtn').disabled=false;$('releaseStatus').textContent=`Support policy saved, but release-state sync failed: ${releaseResult.error.message}`;return}
  config.minimum_supported_version=minimum;
  config.current_release=current;
  await sb.from('admin_audit_log').insert({actor_user_id:me.id,action:'release_policy_updated',target_type:'release',target_id:String(current.versionCode),details:{current_release:current.versionName,minimum_supported_version:minimum.versionName,minimum_supported_code:minimum.versionCode,force_update:minimum.forceUpdate}});
  $('releaseStatus').textContent=`Release policy published against verified OTA ${current.versionName} (${current.versionCode}).`;
  $('saveReleaseBtn').disabled=false;
  await Promise.all([loadAudit(),loadMetrics(),loadReleaseRollout()]);
};

refreshVerifiedOtaRelease().catch(()=>{});
