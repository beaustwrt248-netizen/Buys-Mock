import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!;
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
const DRIVE_FOLDER_ID = Deno.env.get('GOOGLE_DRIVE_BACKUP_FOLDER_ID') || '';
const BACKUP_SECRET = Deno.env.get('MORLEY_BACKUP_SECRET') || '';

// Preferred for a normal personal My Drive folder. The refresh token represents
// the user who owns the folder, so uploaded files consume that user's Drive quota.
const DRIVE_OAUTH_CLIENT_ID = Deno.env.get('GOOGLE_DRIVE_OAUTH_CLIENT_ID') || '';
const DRIVE_OAUTH_CLIENT_SECRET = Deno.env.get('GOOGLE_DRIVE_OAUTH_CLIENT_SECRET') || '';
const DRIVE_OAUTH_REFRESH_TOKEN = Deno.env.get('GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN') || '';

// Retained only as a fallback for deployments using a Google Workspace Shared Drive.
const DRIVE_CLIENT_EMAIL = Deno.env.get('GOOGLE_DRIVE_CLIENT_EMAIL') || '';
const DRIVE_PRIVATE_KEY = (Deno.env.get('GOOGLE_DRIVE_PRIVATE_KEY') || '').replace(/\\n/g, '\n');

const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });

const DEFAULT_TABLES = [
  'profiles','app_config','valuation_history','devices','laptop_models',
  'support_tickets','support_ticket_messages','support_ticket_events',
  'support_ticket_internal_notes','support_ticket_attachments','admin_audit_log',
  'announcements','notification_jobs'
];

function b64url(input: Uint8Array | string) {
  const bytes = typeof input === 'string' ? new TextEncoder().encode(input) : input;
  let binary = ''; for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
}
function pemBytes(pem: string) {
  const raw = pem.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g,'');
  const bin = atob(raw); return Uint8Array.from(bin, c => c.charCodeAt(0));
}
function safeError(error: unknown) {
  if (error instanceof Error) return error.message;
  if (error && typeof error === 'object') {
    const candidate = error as Record<string, unknown>;
    return String(candidate.message || candidate.error_description || candidate.error || candidate.code || 'Unknown backend error');
  }
  return String(error);
}

async function oauthUserToken() {
  if (!DRIVE_OAUTH_CLIENT_ID || !DRIVE_OAUTH_CLIENT_SECRET || !DRIVE_OAUTH_REFRESH_TOKEN) {
    throw new Error('Google Drive user OAuth is not configured');
  }
  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: {'Content-Type':'application/x-www-form-urlencoded'},
    body: new URLSearchParams({
      client_id: DRIVE_OAUTH_CLIENT_ID,
      client_secret: DRIVE_OAUTH_CLIENT_SECRET,
      refresh_token: DRIVE_OAUTH_REFRESH_TOKEN,
      grant_type: 'refresh_token'
    })
  });
  const json = await res.json();
  if (!res.ok || !json.access_token) {
    throw new Error(`Google OAuth refresh failed: ${json.error_description || json.error || res.status}`);
  }
  return json.access_token as string;
}

async function serviceAccountToken() {
  if (!DRIVE_CLIENT_EMAIL || !DRIVE_PRIVATE_KEY) throw new Error('Google Drive service account is not configured');
  const now = Math.floor(Date.now()/1000);
  const header = b64url(JSON.stringify({alg:'RS256',typ:'JWT'}));
  const claim = b64url(JSON.stringify({iss:DRIVE_CLIENT_EMAIL,scope:'https://www.googleapis.com/auth/drive.file',aud:'https://oauth2.googleapis.com/token',iat:now,exp:now+3600}));
  const key = await crypto.subtle.importKey('pkcs8', pemBytes(DRIVE_PRIVATE_KEY), {name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'}, false, ['sign']);
  const signature = new Uint8Array(await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, new TextEncoder().encode(`${header}.${claim}`)));
  const assertion = `${header}.${claim}.${b64url(signature)}`;
  const res = await fetch('https://oauth2.googleapis.com/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion})});
  const json = await res.json();
  if(!res.ok || !json.access_token) throw new Error(`Google service-account token request failed: ${json.error_description || json.error || res.status}`);
  return json.access_token as string;
}

async function googleToken() {
  if (!DRIVE_FOLDER_ID) throw new Error('Google Drive backup folder is not configured');
  if (DRIVE_OAUTH_CLIENT_ID && DRIVE_OAUTH_CLIENT_SECRET && DRIVE_OAUTH_REFRESH_TOKEN) {
    return { token: await oauthUserToken(), authMode: 'user-oauth' as const };
  }
  if (DRIVE_CLIENT_EMAIL && DRIVE_PRIVATE_KEY) {
    return { token: await serviceAccountToken(), authMode: 'service-account' as const };
  }
  throw new Error('Google Drive credentials are not configured');
}

async function sha256(text: string) { return Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(text)))).map(b=>b.toString(16).padStart(2,'0')).join(''); }
async function collectTable(table: string) {
  const rows: unknown[] = []; let from=0; const size=1000;
  while(true){ const {data,error}=await admin.from(table).select('*').range(from,from+size-1); if(error) throw new Error(`Export ${table} failed: ${safeError(error)}`); rows.push(...(data||[])); if(!data || data.length<size) break; from += size; }
  return {table,rows};
}
async function upload(name:string, payload:string, token:string){
  const boundary=`morley_${crypto.randomUUID()}`;
  const meta=JSON.stringify({name,parents:[DRIVE_FOLDER_ID],mimeType:'application/json'});
  const body=`--${boundary}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${meta}\r\n--${boundary}\r\nContent-Type: application/json\r\n\r\n${payload}\r\n--${boundary}--`;
  const res=await fetch('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,createdTime,size',{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':`multipart/related; boundary=${boundary}`},body});
  const json=await res.json(); if(!res.ok) throw new Error(`Drive upload failed: ${json.error?.message || res.status}`); return json;
}

Deno.serve(async(req:Request)=>{
  const reply=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
  if(req.method!=='POST') return reply({error:'POST required'},405);
  let phase='authorize';
  try{
    const supplied=req.headers.get('x-morley-backup-secret') || '';
    let actor:string|null=null;
    if(BACKUP_SECRET && supplied && supplied===BACKUP_SECRET){ actor='scheduler'; }
    else {
      const token=(req.headers.get('Authorization')||'').replace(/^Bearer\s+/i,'');
      if(!token) return reply({error:'Authentication required'},401);
      const {data:{user},error}=await admin.auth.getUser(token); if(error||!user) return reply({error:'Invalid session'},401);
      const {data:profile}=await admin.from('profiles').select('role,is_enabled').eq('id',user.id).single();
      if(!profile?.is_enabled || profile.role!=='admin') return reply({error:'Administrator access required'},403); actor=user.id;
    }
    const body=await req.json().catch(()=>({}));
    if(body?.action && body.action!=='backup') return reply({error:'Restore is deliberately not available from this function'},400);
    const tables=Array.isArray(body?.tables) && body.tables.length ? body.tables.filter((x:unknown)=>DEFAULT_TABLES.includes(String(x))) : DEFAULT_TABLES;
    phase='export';
    const exported=[]; for(const table of tables) exported.push(await collectTable(table));
    const createdAt=new Date().toISOString();
    const document={format:'morley-backup-v1',created_at:createdAt,source_project:new URL(SUPABASE_URL).hostname,tables:Object.fromEntries(exported.map(x=>[x.table,x.rows]))};
    const canonical=JSON.stringify(document); const digest=await sha256(canonical); const envelope=JSON.stringify({...document,sha256:digest},null,2);
    const name=`morley-backup-${createdAt.replace(/[:.]/g,'-')}.json`;
    phase='google-auth';
    const google=await googleToken();
    phase='drive-upload';
    const drive=await upload(name,envelope,google.token);
    phase='audit';
    const {error:auditError}=await admin.from('admin_audit_log').insert({actor_user_id:actor==='scheduler'?null:actor,action:'google_drive_backup_created',target_type:'backup',target_id:drive.id,details:{name,sha256:digest,tables:tables.length,trigger:actor,google_auth_mode:google.authMode}});
    if(auditError) console.error('Backup audit insert failed', safeError(auditError));
    return reply({ok:true,name,file_id:drive.id,sha256:digest,created_at:createdAt,google_auth_mode:google.authMode});
  }catch(error){ return reply({error:safeError(error),phase},500); }
});
