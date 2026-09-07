(()=>{
  const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
  const ENDPOINT=`${SUPABASE_URL}/functions/v1/google-drive-oauth-config`;
  async function load(){
    let session=null;try{session=JSON.parse(localStorage.getItem('morley_web_auth')||'null')}catch{}
    const token=session?.access_token||'';
    if(!token)return;
    try{
      const response=await fetch(ENDPOINT,{method:'POST',headers:{'Content-Type':'application/json','Authorization':`Bearer ${token}`},body:'{}'});
      if(!response.ok)return;
      const data=await response.json();
      if(data?.configured&&data?.client_id)window.MORLEY_GOOGLE_CLIENT_ID=String(data.client_id);
    }catch{}
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',load,{once:true});else load();
})();
