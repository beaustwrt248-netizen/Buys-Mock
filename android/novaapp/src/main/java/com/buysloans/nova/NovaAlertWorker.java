package com.buysloans.nova;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Locale;

public final class NovaAlertWorker extends Worker {
    private static final String PREFS = "nova_alert_state";
    private static final String LAST_DIGEST = "last_digest";

    public NovaAlertWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            NovaCredentialStore store = new NovaCredentialStore(context);
            if (!store.hasRememberedSession()) return Result.success();
            String refresh = store.refreshToken();
            if (refresh == null || refresh.isBlank()) return Result.success();

            NovaApiClient api = new NovaApiClient();
            NovaApiClient.Session session = api.restoreSession(refresh);
            try { store.save(session); } catch (Exception ignored) {}

            JSONArray guardian = api.guardian();
            JSONArray support = api.support();
            JSONArray catalogue = api.catalogue();
            AlertSnapshot snapshot = snapshot(guardian, support, catalogue);
            String digest = snapshot.digest();
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String previous = prefs.getString(LAST_DIGEST, "");
            if (!digest.equals(previous) && snapshot.actionable()) {
                NovaAndroidOperator.postAttentionNotification(
                        context,
                        "Nova found items needing attention",
                        snapshot.summary(),
                        8201);
            }
            prefs.edit().putString(LAST_DIGEST, digest).apply();
            return Result.success();
        } catch (SecurityException e) {
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private static AlertSnapshot snapshot(JSONArray guardian, JSONArray support, JSONArray catalogue) {
        int approvals = 0, highRisk = 0, urgentTickets = 0, overdue = 0, catalogueGaps = 0;
        long now = System.currentTimeMillis();

        for (int i = 0; i < guardian.length(); i++) {
            JSONObject row = guardian.optJSONObject(i);
            if (row == null) continue;
            String state = row.optString("state").toLowerCase(Locale.ROOT);
            if (terminalGuardian(state)) continue;
            if (row.optBoolean("requires_approval")) approvals++;
            String risk = row.optString("risk_level").toLowerCase(Locale.ROOT);
            if (risk.equals("high") || risk.equals("critical")) highRisk++;
        }

        for (int i = 0; i < support.length(); i++) {
            JSONObject row = support.optJSONObject(i);
            if (row == null) continue;
            String state = row.optString("status").toLowerCase(Locale.ROOT);
            if (terminalSupport(state)) continue;
            String priority = row.optString("priority").toLowerCase(Locale.ROOT);
            if (priority.equals("high") || priority.equals("urgent")) urgentTickets++;
            String due = row.optString("sla_due_at");
            if (!due.isBlank()) {
                try { if (Instant.parse(due).toEpochMilli() < now) overdue++; } catch (Exception ignored) {}
            }
        }

        for (int i = 0; i < catalogue.length(); i++) {
            JSONObject row = catalogue.optJSONObject(i);
            if (row == null) continue;
            if (row.optString("model_number").isBlank() || row.optString("image_reference_url").isBlank()) catalogueGaps++;
        }
        return new AlertSnapshot(approvals, highRisk, urgentTickets, overdue, catalogueGaps);
    }

    private static boolean terminalGuardian(String state) {
        return state.equals("resolved") || state.equals("ignored") || state.equals("cancelled")
                || state.equals("closed") || state.equals("dismissed");
    }

    private static boolean terminalSupport(String state) {
        return state.equals("resolved") || state.equals("closed") || state.equals("cancelled") || state.equals("dismissed");
    }

    private static final class AlertSnapshot {
        final int approvals, highRisk, urgentTickets, overdue, catalogueGaps;
        AlertSnapshot(int approvals, int highRisk, int urgentTickets, int overdue, int catalogueGaps) {
            this.approvals = approvals;
            this.highRisk = highRisk;
            this.urgentTickets = urgentTickets;
            this.overdue = overdue;
            this.catalogueGaps = catalogueGaps;
        }
        boolean actionable() { return approvals + highRisk + urgentTickets + overdue > 0; }
        String digest() { return approvals + ":" + highRisk + ":" + urgentTickets + ":" + overdue + ":" + catalogueGaps; }
        String summary() {
            return approvals + " Guardian approvals, " + highRisk + " high-risk incidents, "
                    + urgentTickets + " urgent support tickets and " + overdue + " overdue SLAs."
                    + (catalogueGaps > 0 ? " Catalogue gaps: " + catalogueGaps + "." : "");
        }
    }
}
