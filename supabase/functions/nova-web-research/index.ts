import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
const BRAVE_KEY = Deno.env.get("BRAVE_SEARCH_API_KEY") || "";
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const ALLOWED_ORIGINS = new Set(["https://buyshub.me", "https://www.buyshub.me", "https://beaustwrt248-netizen.github.io"]);
const OFFICIAL_HOSTS: Record<string,string[]> = {
  apple:["apple.com"], samsung:["samsung.com"], google:["store.google.com","support.google.com"], motorola:["motorola.com.au","motorola.com"],
  moto:["motorola.com.au","motorola.com"], oppo:["oppo.com"], oneplus:["oneplus.com"], xiaomi:["mi.com"], vivo:["vivo.com"], realme:["realme.com"],
  huawei:["huawei.com"], lenovo:["lenovo.com"], sony:["sony.com.au","sony.com"], zte:["zte.com.cn","zte.com"], tcl:["tcl.com"],
  nintendo:["nintendo.com.au","nintendo.com"], playstation:["playstation.com"], sonyplaystation:["playstation.com"], microsoft:["xbox.com","microsoft.com"], xbox:["xbox.com"]
};

function headers(req: Request) {
  const origin = req.headers.get("Origin") || "";
  return {
    "Content-Type":"application/json",
    "Access-Control-Allow-Origin": ALLOWED_ORIGINS.has(origin) ? origin : "https://buyshub.me",
    "Access-Control-Allow-Headers":"authorization, x-client-info, apikey, content-type",
    "Access-Control-Allow-Methods":"POST, OPTIONS",
    "Cache-Control":"no-store",
    "Vary":"Origin",
    "X-Content-Type-Options":"nosniff"
  };
}
const clean=(v:unknown,max=300)=>String(v??"").trim().replace(/\s+/g," ").slice(0,max);

async function authorise(req: Request) {
  if (!SUPABASE_URL || !SERVICE_ROLE) return {error:"Nova research backend configuration unavailable",status:503};
  const token=(req.headers.get("Authorization")||"").replace(/^Bearer\s+/i,"");
  if(!token) return {error:"Authentication required",status:401};
  const {data:{user},error}=await admin.auth.getUser(token);
  if(error||!user) return {error:"Invalid session",status:401};
  const {data:profile,error:profileError}=await admin.from("profiles").select("role,is_enabled").eq("id",user.id).maybeSingle();
  if(profileError) throw profileError;
  if(!profile?.is_enabled || !["admin","manager"].includes(profile.role)) return {error:"Admin or Manager access required",status:403};
  return {user,profile};
}

function officialFor(query:string, host:string) {
  const q=query.toLowerCase();
  for(const [brand,hosts] of Object.entries(OFFICIAL_HOSTS)) {
    if(q.includes(brand) && hosts.some(h=>host===h || host.endsWith("."+h))) return true;
  }
  return false;
}
function score(query:string,url:string) {
  try {
    const h=new URL(url).hostname.toLowerCase().replace(/^www\./,"");
    let s=0;
    if(officialFor(query,h)) s+=100;
    if(h.endsWith(".com.au")||h.endsWith(".au")) s+=35;
    if(/support\.|manual|spec|product|device/.test(url.toLowerCase())) s+=8;
    if(/reddit\.com|pinterest\.|quora\.|medium\./.test(h)) s-=100;
    return s;
  } catch { return -100; }
}

async function brave(query:string,count:number) {
  if(!BRAVE_KEY) throw new Error("BRAVE_SEARCH_API_KEY missing");
  const u=new URL("https://api.search.brave.com/res/v1/web/search");
  u.searchParams.set("q",query);
  u.searchParams.set("country","AU");
  u.searchParams.set("search_lang","en");
  u.searchParams.set("ui_lang","en-AU");
  u.searchParams.set("count",String(Math.min(Math.max(count,5),20)));
  u.searchParams.set("safesearch","moderate");
  const r=await fetch(u,{headers:{Accept:"application/json","X-Subscription-Token":BRAVE_KEY}});
  if(!r.ok) throw new Error(`Brave Search ${r.status}`);
  return await r.json();
}

Deno.serve(async(req:Request)=>{
  const h=headers(req), reply=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers:h});
  if(req.method==="OPTIONS") return new Response("ok",{headers:h});
  if(req.method!=="POST") return reply({error:"POST required"},405);
  try {
    const auth=await authorise(req); if("error" in auth) return reply({error:auth.error},auth.status);
    let body:any={}; try{body=await req.json();}catch{return reply({error:"Invalid JSON request"},400);}
    const query=clean(body.query??body.q,240); if(!query) return reply({error:"Research query required"},400);
    const mode=clean(body.mode,30).toLowerCase()||"general";
    const deviceMode=mode==="device"||/model|device|phone|tablet|watch|console|laptop|spec|storage|release year/i.test(query);
    const searchQuery=deviceMode ? `${query} Australia official specifications model manual` : `${query} Australia`;
    const data=await brave(searchQuery,Number(body.limit)||12);
    const raw=(data.web?.results||[]).map((x:any)=>{
      let host=""; try{host=new URL(x.url).hostname.replace(/^www\./,"");}catch{}
      return {title:clean(x.title,220),url:x.url||"",host,snippet:clean(x.description,520),score:score(query,x.url||""),official:officialFor(query,host),australian:host.endsWith(".au")};
    }).filter((x:any)=>x.url&&x.title&&x.score>-50);
    raw.sort((a:any,b:any)=>b.score-a.score);
    const seen=new Set<string>(), results:any[]=[];
    for(const x of raw){if(seen.has(x.url))continue;seen.add(x.url);results.push({...x,citation:results.length+1});if(results.length>=10)break;}
    return reply({ok:true,query,mode:deviceMode?"device":"general",checked_at:new Date().toISOString(),source:"Brave Search",policy:{country:"AU",manufacturer_first:deviceMode,rejects_reddit:true,protected_actions:"advisory-only"},results});
  } catch(error) {
    const message=error instanceof Error?error.message:String(error);
    console.error(`[nova-web-research] ${message}`);
    return reply({error:message},500);
  }
});
