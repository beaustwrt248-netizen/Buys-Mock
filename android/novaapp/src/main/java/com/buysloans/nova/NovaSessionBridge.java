package com.buysloans.nova;

final class NovaSessionBridge {
    private static volatile NovaApiClient api;

    private NovaSessionBridge() {}

    static void attach(NovaApiClient client) { api = client; }
    static NovaApiClient api() { return api; }
    static void clear() { api = null; }
}
