import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const WEB_ORIGIN = "https://beaustwrt248-netizen.github.io";

function cors(req: Request) {
  const origin = req.headers.get("Origin") || "";
  return {
    "Access-Control-Allow-Origin": origin === WEB_ORIGIN ? origin : WEB_ORIGIN,
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Vary": "Origin",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "no-referrer",
  };
}

async function sha256(value: string) {
  const bytes = new TextEncoder().encode(value.trim());
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

function constantTimeEqual(left: string, right: string) {
  if (left.length !== right.length) return false;
  let difference = 0;
  for (let index = 0; index < left.length; index += 1) {
    difference |= left.charCodeAt(index) ^ right.charCodeAt(index);
  }
  return difference === 0;
}

async function verifyTurnstile(token: string) {
  const secret = Deno.env.get("TURNSTILE_SECRET_KEY") ?? Deno.env.get("CAPTCHA_SECRET") ?? "";
  if (!secret) return { ok: false, error: "Turnstile server secret is not configured." };
  if (!token) return { ok: false, error: "Complete the security check first." };
  const form = new FormData();
  form.set("secret", secret);
  form.set("response", token);
  const response = await fetch("https://challenges.cloudflare.com/turnstile/v0/siteverify", { method: "POST", body: form });
  const result = await response.json();
  return { ok: result.success === true, error: result.success ? "" : "Security check failed. Please try again." };
}

Deno.serve(async (req) => {
  const headers = cors(req);
  const json = (body: unknown, status = 200) =>
    new Response(JSON.stringify(body), { status, headers: { ...headers, "Content-Type": "application/json" } });

  if (req.method === "OPTIONS") return new Response("ok", { headers });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  try {
    const { email, password, inviteCode, captchaToken } = await req.json();
    const cleanEmail = String(email ?? "").trim().toLowerCase();
    const cleanPassword = String(password ?? "");
    const cleanCode = String(inviteCode ?? "").trim();

    if (!cleanEmail || !cleanCode || cleanPassword.length < 10) {
      return json({ error: "Enter the approved email, invite code, and a password of at least 10 characters." }, 400);
    }
    if (cleanEmail.length > 254 || cleanCode.length > 80 || cleanPassword.length > 256) {
      return json({ error: "The supplied account details are invalid." }, 400);
    }

    const url = Deno.env.get("SUPABASE_URL")!;
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const admin = createClient(url, serviceKey, { auth: { persistSession: false } });
    const forwarded = req.headers.get("cf-connecting-ip") ?? req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ?? "unknown";
    const rateSecret = Deno.env.get("INVITE_RATE_LIMIT_SECRET") ?? serviceKey;
    const rateKeyHash = await sha256(`${rateSecret}|${forwarded}|${cleanEmail}`);
    const { data: rateAccepted, error: rateError } = await admin.rpc("consume_invite_redemption_rate_limit", {
      rate_key_hash: rateKeyHash,
      maximum_attempts: 5,
      window_seconds: 900,
    });
    if (rateError) throw rateError;
    if (rateAccepted !== true) return json({ error: "Too many attempts. Wait 15 minutes and try again." }, 429);

    const captcha = await verifyTurnstile(String(captchaToken ?? ""));
    if (!captcha.ok) return json({ error: captcha.error }, 403);

    const hash = await sha256(cleanCode);
    const { data: invite, error: inviteError } = await admin
      .from("app_invites")
      .select("id,email,display_name,role,code_hash,expires_at,used_at")
      .ilike("email", cleanEmail)
      .is("used_at", null)
      .order("created_at", { ascending: false })
      .limit(1)
      .maybeSingle();

    if (inviteError) throw inviteError;
    if (!invite || !constantTimeEqual(String(invite.code_hash ?? ""), hash) || new Date(invite.expires_at).getTime() <= Date.now()) {
      return json({ error: "This email is not approved or the invite code is invalid/expired." }, 403);
    }

    const fullName = String(invite.display_name ?? "").trim().replace(/\s+/g, " ");
    const parts = fullName.split(" ").filter(Boolean);
    const firstName = parts[0] || "";
    const lastName = parts.slice(1).join(" ");
    if (!firstName || !lastName || fullName.length > 100) {
      return json({ error: "This invite is missing the approved first and last name. Ask an administrator to create a new invite." }, 403);
    }

    const { data: created, error: createError } = await admin.auth.admin.createUser({
      email: cleanEmail,
      password: cleanPassword,
      email_confirm: true,
      user_metadata: { first_name: firstName, last_name: lastName, full_name: fullName, invited: true },
    });
    if (createError) {
      const message = createError.message?.toLowerCase().includes("already")
        ? "An account already exists for this email."
        : createError.message;
      return json({ error: message }, 400);
    }

    const userId = created.user.id;
    const { error: profileError } = await admin.from("profiles").upsert({
      id: userId,
      email: cleanEmail,
      display_name: fullName,
      role: invite.role ?? "staff",
      is_enabled: true,
      updated_at: new Date().toISOString(),
    }, { onConflict: "id" });
    if (profileError) {
      await admin.auth.admin.deleteUser(userId);
      throw profileError;
    }

    const { data: marked, error: markError } = await admin
      .from("app_invites")
      .update({ used_at: new Date().toISOString() })
      .eq("id", invite.id)
      .is("used_at", null)
      .select("id")
      .maybeSingle();
    if (markError || !marked) {
      await admin.auth.admin.deleteUser(userId);
      throw markError ?? new Error("Invite was already used.");
    }

    return json({ ok: true, message: "Account created. You can sign in now." });
  } catch (error) {
    console.error(error instanceof Error ? error.name : "invite_redemption_error");
    return json({ error: "Unable to create account right now." }, 500);
  }
});
