package com.buysloans.nova;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NovaAlertWorker extends JobService {
    private static final String PREFS = "nova_alert_state";
    private static final String LAST_DIGEST = "last_digest";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override
    public boolean onStartJob(JobParameters params) {
        worker.execute(() -> {
            boolean retry = false;
            try {
                runSnapshot();
            } catch (SecurityException ignored) {
                retry = false;
            } catch (Exception ignored) {
                retry = true;
            }
            jobFinished(params, retry);
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    @Override
    public void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private void runSnapshot() throws Exception {
        Context context = getApplicationContext();
        NovaCredentialStore store = new NovaCredentialStore(context);
        if (!store.hasRememberedSession()) return;
        String refresh = store.refreshToken();
        if (refresh == null || refresh.isBlank()) return;

        NovaApiClient api = new NovaApiClient();
        NovaApiClient.Session session = api.restoreSession(refresh);
        try { store.save(session); } catch (Exception ignored) {}

        AlertSnapshot snapshot = snapshot(api.guardian(), api.support(), api.catalogue());
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
