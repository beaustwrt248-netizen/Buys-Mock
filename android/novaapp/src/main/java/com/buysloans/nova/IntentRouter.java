package com.buysloans.nova;

import java.util.Locale;

final class IntentRouter {
    enum Intent { GREETING, CAPABILITIES, PERFORMANCE, INVENTORY, GUARDIAN, SUPPORT, RELEASES, UNKNOWN }

    private IntentRouter() {}

    static Intent classify(String text) {
        String x = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (x.isEmpty()) return Intent.UNKNOWN;
        if (x.matches("(hi|hey|hello|hiya|yo|howdy)( nova)?")) return Intent.GREETING;
        if (x.contains("what can you do") || x.contains("capabilities") || x.contains("help me use nova")) return Intent.CAPABILITIES;
        if (containsAny(x, "guardian", "approval", "protected repair", "incident")) return Intent.GUARDIAN;
        if (containsAny(x, "support", "ticket", "sla")) return Intent.SUPPORT;
        if (containsAny(x, "inventory", "stock", "in stock")) return Intent.INVENTORY;
        if (containsAny(x, "release", "deployment", "version", "apk", "ota")) return Intent.RELEASES;
        if (containsAny(x, "sales", "profit", "performance", "revenue", "margin", "valuation")) return Intent.PERFORMANCE;
        return Intent.UNKNOWN;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
