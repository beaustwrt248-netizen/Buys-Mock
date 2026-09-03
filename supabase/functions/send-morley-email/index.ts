import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY") ?? "";
const RESEND_FROM_EMAIL = Deno.env.get("RESEND_FROM_EMAIL") ?? "B&L Morley <noreply@buyshub.me>";
const RESEND_REPLY_TO = Deno.env.get("RESEND_REPLY_TO") ?? "";
const APP_NAME = "B&L Morley";
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });
const ALLOWED_ORIGINS = new Set(["https://buyshub.me", "https://www.buyshub.me", "https://beaustwrt248-netizen.github.io"]);

function corsHeaders(req: Request) {
  const origin = req.headers.get("Origin") || "";
  return {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": ALLOWED_ORIGINS.has(origin) ? origin : "https://buyshub.me",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    Vary: "Origin",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "no-referrer",
  };
}

const clean = (value: unknown, max = 5000) => String(value ?? "").trim().slice(0, max);
const validEmail = (value: string) => value.length >= 3 && value.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
const escapeHtml = (value: unknown) => String(value ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));

async function sha256(value: string) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
}
function generateInviteCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = crypto.getRandomValues(new Uint8Array(12));
  return "BLM-" + [0, 1, 2].map((group) => [0, 1, 2, 3].map((offset) => chars[bytes[group * 4 + offset] % chars.length]).join("")).join("-");
}
function formatPerth(value: string) {
  return new Intl.DateTimeFormat("en-AU", { dateStyle: "medium", timeStyle: "short", timeZone: "Australia/Perth" }).format(new Date(value));
}

async function sendEmail(to: string[], subject: string, htmlBody: string, textBody: string, type: string) {
  if (!RESEND_API_KEY) throw new Error("RESEND_API_KEY is not configured");
  const senderAddress = RESEND_FROM_EMAIL.replace(/^.*<([^>]+)>.*$/, "$1");
  if (!RESEND_FROM_EMAIL || !validEmail(senderAddress)) throw new Error("RESEND_FROM_EMAIL is not configured with a valid sender");
  const recipients = Array.from(new Set(to.map((value) => value.trim().toLowerCase()).filter(validEmail))).slice(0, 100);
  if (!recipients.length) throw new Error("No eligible email recipients found");
  const payload: Record<string, unknown> = {
    from: RESEND_FROM_EMAIL,
    to: recipients,
    subject: subject.slice(0, 180),
    html: htmlBody,
    text: textBody,
    tags: [{ name: "type", value: type.slice(0, 256) }],
  };
  if (RESEND_REPLY_TO && validEmail(RESEND_REPLY_TO)) payload.reply_to = RESEND_REPLY_TO;
  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${RESEND_API_KEY}` },
    body: JSON.stringify(payload),
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(`Resend ${response.status}: ${clean((result as any)?.message || "Email delivery failed", 500)}`);
  return { id: clean((result as any)?.id, 120) || null, recipients: recipients.length };
}

async function getCaller(req: Request) {
  const token = (req.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
  if (!token) return null;
  const { data: { user }, error } = await admin.auth.getUser(token);
  if (error || !user) return null;
  const { data: profile } = await admin.from("profiles").select("id,email,display_name,role,is_enabled").eq("id", user.id).maybeSingle();
  if (!profile?.is_enabled) return null;
  return { user, profile };
}
async function activeAdminEmails() {
  const { data, error } = await admin.from("profiles").select("email").eq("is_enabled", true).in("role", ["admin", "manager"]);
  if (error) throw error;
  return (data ?? []).map((profile) => String(profile.email ?? "").toLowerCase()).filter(validEmail);
}
async function writeAudit(actorId: string, action: string, targetType: string, targetId: string | null, details: Record<string, unknown>) {
  await admin.from("admin_audit_log").insert({ actor_user_id: actorId, action, target_type: targetType, target_id: targetId, details });
}
function inviteMail(displayName: string, role: string, code: string, expiresAt: string) {
  const subject = `${APP_NAME} invitation`;
  const text = `Hi ${displayName},\n\nYou've been invited to ${APP_NAME} as ${role}.\n\nInvite code: ${code}\n\nOpen ${APP_NAME}, choose Create account, and enter the approved email address plus this invite code. The code expires ${formatPerth(expiresAt)}.\n\nIf you weren't expecting this invitation, you can ignore this email.`;
  const html = `<div style="font-family:Arial,sans-serif;max-width:620px;margin:auto"><h2>${APP_NAME} invitation</h2><p>Hi ${escapeHtml(displayName)},</p><p>You've been invited to <strong>${APP_NAME}</strong> as <strong>${escapeHtml(role)}</strong>.</p><p>Your invite code is:</p><div style="font-size:28px;font-weight:700;letter-spacing:4px;padding:16px;border:1px solid #ddd;border-radius:10px;text-align:center">${escapeHtml(code)}</div><p>Open ${APP_NAME}, choose <strong>Create account</strong>, and enter your approved email address plus this invite code.</p><p>This code expires <strong>${escapeHtml(formatPerth(expiresAt))}</strong>.</p><p style="color:#666">If you weren't expecting this invitation, you can ignore this email.</p></div>`;
  return { subject, text, html };
}

Deno.serve(async (req: Request) => {
  const headers = corsHeaders(req);
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers });
  if (req.method === "OPTIONS") return new Response("ok", { headers });
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);
  try {
    const caller = await getCaller(req);
    if (!caller) return reply({ error: "Authentication required" }, 401);
    const body = await req.json();
    const action = clean(body?.action, 80);
    const callerRole = String(caller.profile.role || "").toLowerCase();
    const isAdminControl = ["admin", "manager"].includes(callerRole);

    if (action === "create_invite") {
      if (!isAdminControl) return reply({ error: "Admin or Manager access required" }, 403);
      const email = clean(body?.email, 254).toLowerCase();
      const displayName = clean(body?.display_name, 100).replace(/\s+/g, " ");
      const requestedRole = clean(body?.role, 20).toLowerCase() || "staff";
      if (!validEmail(email)) return reply({ error: "Enter a valid email address" }, 400);
      if (displayName.length < 3 || displayName.split(" ").filter(Boolean).length < 2) return reply({ error: "Enter a valid first and last name" }, 400);
      if (!["staff", "manager"].includes(requestedRole)) return reply({ error: "Invalid role" }, 400);
      if (callerRole === "manager" && requestedRole !== "staff") return reply({ error: "Managers may invite Staff only" }, 403);
      const { data: existing } = await admin.from("profiles").select("id").ilike("email", email).limit(1);
      if ((existing ?? []).length) return reply({ error: "An account already exists for this email." }, 409);
      const inviteCode = generateInviteCode();
      const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();
      const { data: invite, error: inviteError } = await admin.from("app_invites").insert({
        email, display_name: displayName, role: requestedRole, code_hash: await sha256(inviteCode), expires_at: expiresAt, created_by: caller.user.id,
      }).select("id,email,display_name,role,expires_at,used_at,created_at").single();
      if (inviteError) throw inviteError;
      try {
        const mail = inviteMail(displayName, requestedRole, inviteCode, expiresAt);
        const sent = await sendEmail([email], mail.subject, mail.html, mail.text, "invite");
        await writeAudit(caller.user.id, "email_invite_sent", "app_invite", invite.id, { email, role: requestedRole, resend_id: sent.id });
        return reply({ ok: true, action, invite, invite_code: inviteCode, expires_at: expiresAt });
      } catch (error) {
        await admin.from("app_invites").delete().eq("id", invite.id).eq("created_by", caller.user.id);
        throw error;
      }
    }

    if (action === "reissue_invite") {
      if (!isAdminControl) return reply({ error: "Admin or Manager access required" }, 403);
      const inviteId = clean(body?.invite_id, 80);
      const { data: invite, error } = await admin.from("app_invites").select("id,email,display_name,role,expires_at,used_at,created_at").eq("id", inviteId).maybeSingle();
      if (error) throw error;
      if (!invite || invite.used_at) return reply({ error: "Invite is no longer active" }, 409);
      if (callerRole === "manager" && invite.role !== "staff") return reply({ error: "Managers may reissue Staff invites only" }, 403);
      const inviteCode = generateInviteCode();
      const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();
      const { data: updated, error: updateError } = await admin.from("app_invites").update({ code_hash: await sha256(inviteCode), expires_at: expiresAt }).eq("id", invite.id).select("id,email,display_name,role,expires_at,used_at,created_at").single();
      if (updateError) throw updateError;
      const mail = inviteMail(invite.display_name, invite.role, inviteCode, expiresAt);
      const sent = await sendEmail([invite.email], mail.subject, mail.html, mail.text, "invite_reissued");
      await writeAudit(caller.user.id, "email_invite_reissued", "app_invite", invite.id, { email: invite.email, role: invite.role, resend_id: sent.id });
      return reply({ ok: true, action, invite: updated, invite_code: inviteCode, expires_at: expiresAt });
    }

    if (action === "support_ticket_created") {
      const ticketId = clean(body?.ticket_id, 80);
      if (!ticketId) return reply({ error: "ticket_id required" }, 400);
      const { data: ticket, error } = await admin.from("support_tickets").select("id,user_id,category,subject,description,status,priority,app_version,device_model,created_at").eq("id", ticketId).maybeSingle();
      if (error) throw error;
      if (!ticket) return reply({ error: "Support ticket not found" }, 404);
      if (ticket.user_id !== caller.user.id && !isAdminControl) return reply({ error: "Not allowed for this support ticket" }, 403);
      const { data: owner } = await admin.from("profiles").select("display_name,email").eq("id", ticket.user_id).maybeSingle();
      const recipients = await activeAdminEmails();
      const subject = `[Support] ${clean(ticket.subject, 140)}`;
      const text = `From: ${clean(owner?.display_name || owner?.email || "User", 120)}\nCategory: ${clean(ticket.category, 80)}\nPriority: ${clean(ticket.priority, 40)}\nApp: ${clean(ticket.app_version, 40)}\nDevice: ${clean(ticket.device_model, 100)}\n\n${clean(ticket.description, 4000)}`;
      const html = `<div style="font-family:Arial,sans-serif;max-width:680px;margin:auto"><h2>New support ticket</h2><p><strong>${escapeHtml(ticket.subject)}</strong></p><table cellpadding="6"><tr><td>From</td><td>${escapeHtml(owner?.display_name || owner?.email || "User")}</td></tr><tr><td>Category</td><td>${escapeHtml(ticket.category)}</td></tr><tr><td>Priority</td><td>${escapeHtml(ticket.priority)}</td></tr><tr><td>App</td><td>${escapeHtml(ticket.app_version || "-")}</td></tr><tr><td>Device</td><td>${escapeHtml(ticket.device_model || "-")}</td></tr></table><p>${escapeHtml(ticket.description).replace(/\n/g, "<br>")}</p><p>Ticket ID: <code>${escapeHtml(ticket.id)}</code></p></div>`;
      const sent = await sendEmail(recipients, subject, html, text, "support_created");
      return reply({ ok: true, action, ticket_id: ticket.id, recipients: sent.recipients });
    }

    if (action === "support_ticket_reply") {
      const ticketId = clean(body?.ticket_id, 80);
      const messageId = clean(body?.message_id, 80);
      if (!ticketId || !messageId) return reply({ error: "ticket_id and message_id required" }, 400);
      const [{ data: ticket, error: ticketError }, { data: message, error: messageError }] = await Promise.all([
        admin.from("support_tickets").select("id,user_id,subject,status").eq("id", ticketId).maybeSingle(),
        admin.from("support_ticket_messages").select("id,ticket_id,author_user_id,author_role,body,created_at").eq("id", messageId).maybeSingle(),
      ]);
      if (ticketError) throw ticketError;
      if (messageError) throw messageError;
      if (!ticket || !message || message.ticket_id !== ticket.id) return reply({ error: "Support message not found" }, 404);
      if (message.author_user_id !== caller.user.id) return reply({ error: "Only the message author can trigger its email" }, 403);
      if (ticket.user_id !== caller.user.id && !isAdminControl) return reply({ error: "Not allowed for this support ticket" }, 403);
      let recipients: string[] = [];
      if (isAdminControl) {
        const { data: owner } = await admin.from("profiles").select("email").eq("id", ticket.user_id).maybeSingle();
        if (validEmail(String(owner?.email ?? ""))) recipients = [String(owner.email).toLowerCase()];
      } else recipients = await activeAdminEmails();
      const senderName = clean(caller.profile.display_name || caller.profile.email || "Morley user", 120);
      const subject = `Re: ${clean(ticket.subject, 140)}`;
      const text = `${senderName} replied to support ticket ${ticket.id}:\n\n${clean(message.body, 4000)}`;
      const html = `<div style="font-family:Arial,sans-serif;max-width:680px;margin:auto"><h2>Support ticket reply</h2><p><strong>${escapeHtml(ticket.subject)}</strong></p><p>${escapeHtml(senderName)} replied:</p><blockquote style="border-left:4px solid #ddd;padding-left:14px">${escapeHtml(message.body).replace(/\n/g, "<br>")}</blockquote><p>Ticket ID: <code>${escapeHtml(ticket.id)}</code></p></div>`;
      const sent = await sendEmail(recipients, subject, html, text, "support_reply");
      if (isAdminControl) await writeAudit(caller.user.id, "support_email_sent", "support_ticket", ticket.id, { message_id: message.id, resend_id: sent.id });
      return reply({ ok: true, action, ticket_id: ticket.id, message_id: message.id, recipients: sent.recipients });
    }

    if (action === "notification_job") {
      if (!isAdminControl) return reply({ error: "Admin Control access required" }, 403);
      const jobId = clean(body?.job_id, 80);
      if (!jobId) return reply({ error: "job_id required" }, 400);
      const { data: job, error } = await admin.from("notification_jobs").select("id,title,body,audience,target_user_id,target_installation_id,status").eq("id", jobId).maybeSingle();
      if (error) throw error;
      if (!job) return reply({ error: "Notification job not found" }, 404);
      let targetUserId = clean(job.target_user_id, 80) || null;
      if (job.target_installation_id) {
        const { data: device, error: deviceError } = await admin.from("devices").select("user_id").eq("installation_id", job.target_installation_id).maybeSingle();
        if (deviceError) throw deviceError;
        targetUserId = clean(device?.user_id, 80) || null;
        if (!targetUserId) return reply({ error: "Device-only target is not linked to an email recipient" }, 400);
      }
      let query = admin.from("profiles").select("id,email,role,is_enabled").eq("is_enabled", true);
      if (targetUserId) query = query.eq("id", targetUserId);
      else if (job.audience && job.audience !== "all") query = query.eq("role", job.audience);
      const { data: targets, error: targetError } = await query.limit(100);
      if (targetError) throw targetError;
      const recipients = (targets ?? []).map((profile) => String(profile.email ?? "").toLowerCase()).filter(validEmail);
      const subject = clean(job.title, 180);
      const text = clean(job.body, 5000);
      const html = `<div style="font-family:Arial,sans-serif;max-width:680px;margin:auto"><h2>${escapeHtml(job.title)}</h2><p>${escapeHtml(job.body).replace(/\n/g, "<br>")}</p></div>`;
      const sent = await sendEmail(recipients, subject, html, text, "admin_notification");
      await writeAudit(caller.user.id, "notification_email_delivery", "notification_job", job.id, { recipients: sent.recipients, resend_id: sent.id });
      return reply({ ok: true, action, job_id: job.id, recipients: sent.recipients });
    }

    return reply({ error: "Unsupported action" }, 400);
  } catch (error) {
    console.error(error instanceof Error ? error.message : String(error));
    return reply({ error: error instanceof Error ? error.message : "Unable to send email" }, 500);
  }
});
