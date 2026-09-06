package com.buysloans.nova;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class NovaApiClient {
    static final class Session {
        final String accessToken;
        final String refreshToken;
        final String userId;
        final String email;
        Session(String accessToken, String refreshToken, String userId, String email) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.userId = userId;
            this.email = email;
        }
    }

    private Session session;

    Session signIn(String email, String password, String captchaToken) throws Exception {
        if (captchaToken == null || captchaToken.isBlank()) {
            throw new SecurityException("Complete the security check before signing in.");
        }
        JSONObject body = new JSONObject()
                .put("email", email)
                .put("password", password)
                .put("gotrue_meta_security", new JSONObject().put("captcha_token", captchaToken));
        JSONObject json = new JSONObject(request(
                "POST", "/auth/v1/token?grant_type=password", body.toString(), null));
        return acceptSession(json, email);
    }

    Session restoreSession(String refreshToken) throws Exception {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SecurityException("No remembered Nova session is available.");
        }
        JSONObject body = new JSONObject().put("refresh_token", refreshToken);
        JSONObject json = new JSONObject(request(
                "POST", "/auth/v1/token?grant_type=refresh_token", body.toString(), null));
        return acceptSession(json, "");
    }

    private Session acceptSession(JSONObject json, String fallbackEmail) throws Exception {
        JSONObject user = json.getJSONObject("user");
        Session candidate = new Session(
                json.getString("access_token"),
                json.optString("refresh_token"),
                user.getString("id"),
                user.optString("email", fallbackEmail));
        JSONObject profile = first(getWith(candidate,
                "/rest/v1/profiles?select=role,is_enabled&id=eq." + encode(candidate.userId)));
        String role = profile.optString("role");
        if (!profile.optBoolean("is_enabled", false)
                || !("admin".equals(role) || "manager".equals(role))) {
            throw new SecurityException("This account is not authorised for Nova AI.");
        }
        session = candidate;
        return candidate;
    }

    void signOut() { session = null; }
    boolean isSignedIn() { return session != null; }
    String signedInEmail() { return session == null ? "" : session.email; }

    JSONArray sales() throws Exception { return get("/rest/v1/sales_records?select=id,acquired_cost,sold_price,fees,other_costs,realised_profit,sold_at&order=sold_at.desc&limit=100"); }
    JSONArray inventory() throws Exception { return get("/rest/v1/inventory_items?select=id,status,acquired_price,expected_sale_price,acquired_at&order=acquired_at.desc&limit=500"); }
    JSONArray valuations() throws Exception { return get("/rest/v1/valuation_history?select=id,expected_profit,actual_profit,status,created_at&order=created_at.desc&limit=250"); }
    JSONArray guardian() throws Exception { return get("/rest/v1/guardian_incidents?select=id,state,risk_level,requires_approval,classification,occurrence_count,verified_at,updated_at&order=updated_at.desc&limit=250"); }
    JSONArray guardianRepairs() throws Exception { return get("/rest/v1/guardian_repairs?select=id,status,generated_at,tested_at,completed_at,updated_at&order=updated_at.desc&limit=250"); }
    JSONArray support() throws Exception { return get("/rest/v1/support_tickets?select=id,status,priority,assigned_to,sla_due_at,updated_at&order=updated_at.desc&limit=200"); }
    JSONArray catalogue() throws Exception { return get("/rest/v1/device_catalog?select=id,category,brand,model_name,model_number,release_year,ram_options,storage_options,active,updated_at&active=eq.true&order=release_year.desc.nullslast&limit=1000"); }
    JSONArray releaseConfig() throws Exception { return get("/rest/v1/app_config?select=key,value&key=in.(current_release,minimum_supported_version,feature_flags)"); }

    JSONObject knowledgeSearch(String query) throws Exception {
        return edge("nova-knowledge", new JSONObject().put("action", "search").put("q", query).put("limit", 6));
    }

    JSONObject knowledgeSummary() throws Exception {
        return edge("nova-knowledge", new JSONObject().put("action", "summary"));
    }

    JSONObject learningSummary() throws Exception {
        return edge("nova-learning", new JSONObject().put("action", "summary"));
    }

    private JSONArray get(String path) throws Exception {
        if (session == null) throw new SecurityException("Sign in to Nova first.");
        return getWith(session, path);
    }

    private JSONArray getWith(Session useSession, String path) throws Exception {
        return new JSONArray(request("GET", path, null, useSession.accessToken));
    }

    private JSONObject edge(String function, JSONObject body) throws Exception {
        if (session == null) throw new SecurityException("Sign in to Nova first.");
        return new JSONObject(request("POST", "/functions/v1/" + function, body.toString(), session.accessToken));
    }

    private static JSONObject first(JSONArray rows) throws Exception {
        if (rows.length() == 0) throw new SecurityException("Authorised profile was not found.");
        return rows.getJSONObject(0);
    }

    private static String encode(String value) throws Exception { return URLEncoder.encode(value, StandardCharsets.UTF_8.name()); }

    private String request(String method, String path, String body, String bearer) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BuildConfig.SUPABASE_URL + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY);
        connection.setRequestProperty("Accept", "application/json");
        if (bearer != null && !bearer.isBlank()) connection.setRequestProperty("Authorization", "Bearer " + bearer);
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream out = connection.getOutputStream()) { out.write(body.getBytes(StandardCharsets.UTF_8)); }
        }
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = read(stream);
        connection.disconnect();
        if (code < 200 || code >= 300) {
            String message = "Nova request failed (" + code + ")";
            try {
                JSONObject error = new JSONObject(response);
                String detail = error.optString("msg", error.optString("message", error.optString("error_description", error.optString("error"))));
                if (!detail.isBlank()) message += ": " + detail;
            } catch (Exception ignored) {}
            throw new Exception(message);
        }
        return response;
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) value.append(line);
        }
        return value.toString();
    }
}
