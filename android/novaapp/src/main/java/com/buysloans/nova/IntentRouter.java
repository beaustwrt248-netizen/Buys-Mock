package com.buysloans.nova;

import java.util.Locale;

final class IntentRouter {
    enum Intent { GREETING, SMALL_TALK, CAPABILITIES, ATTENTION, PERFORMANCE, INVENTORY, GUARDIAN, SUPPORT, RELEASES, CATALOGUE, LEARNING, UNKNOWN }

    private IntentRouter() {}

    static Intent classify(String text) {
        String x = normalise(text);
        if (x.isEmpty()) return Intent.UNKNOWN;
        if (x.matches("(hi|hey|hello|hiya|yo|howdy|good morning|good afternoon|good evening)( nova)?")) return Intent.GREETING;
        if (containsAny(x,
                "how are you", "how are things", "how s it going", "how is it going", "you good", "are you okay",
                "thanks", "thank you", "cheers", "nice", "awesome", "great", "good job", "well done",
                "who are you", "what are you", "what s your name", "what is your name", "tell me about yourself",
                "what are you doing", "what do you think", "are you there", "good night", "bye", "goodbye", "see you",
                "tell me a joke", "joke", "i m tired", "im tired", "i am tired", "i m bored", "im bored", "i am bored")) return Intent.SMALL_TALK;
        if (x.contains("what can you do") || x.contains("capabilities") || x.contains("help me use nova") || x.contains("how can you help")) return Intent.CAPABILITIES;
        if (containsAny(x,
                "what needs my attention", "what needs attention", "needs attention", "need my attention",
                "what should i look at", "what should i check", "highest priority", "urgent items", "anything urgent")) return Intent.ATTENTION;
        if (containsAny(x, "guardian", "approval", "protected repair", "incident")) return Intent.GUARDIAN;
        if (containsAny(x, "support", "ticket", "sla")) return Intent.SUPPORT;
        if (containsAny(x, "inventory", "stock", "in stock")) return Intent.INVENTORY;
        if (containsAny(x, "catalogue", "catalog", "device list", "model number", "storage options", "release year")) return Intent.CATALOGUE;
        if (containsAny(x, "learn", "learning", "outcome", "pattern", "accuracy", "forecast error", "improve")) return Intent.LEARNING;
        if (containsAny(x, "release", "deployment", "version", "apk", "ota")) return Intent.RELEASES;
        if (containsAny(x, "sales", "profit", "performance", "revenue", "margin", "valuation")) return Intent.PERFORMANCE;
        return Intent.UNKNOWN;
    }

    static boolean isFollowUp(String text) {
        String x = normalise(text);
        if (x.isEmpty() || x.length() >= 120) return false;
        return x.equals("why") || x.equals("how") || x.equals("which") || x.equals("tell me more") ||
                x.equals("more") || x.equals("what else") || x.equals("what about that") || x.equals("really") ||
                x.equals("and that") || x.startsWith("why ") || x.startsWith("which ") ||
                x.startsWith("what about ") || x.startsWith("how about ") || x.startsWith("and ") ||
                x.startsWith("tell me more") || x.startsWith("go deeper") || x.startsWith("explain that");
    }

    static String normalise(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
