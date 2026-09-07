package com.buysloans.nova;

import java.util.Locale;

final class IntentRouter {
    enum Intent { GREETING, SMALL_TALK, CAPABILITIES, DAILY_BRIEF, KNOWLEDGE, ATTENTION, PERFORMANCE, INVENTORY, GUARDIAN, SUPPORT, RELEASES, CATALOGUE, LEARNING, UNKNOWN }

    private IntentRouter() {}

    static Intent classify(String text) {
        String x = normalise(text);
        if (x.isEmpty()) return Intent.UNKNOWN;
        if (x.matches("(hi|hey|hello|hiya|yo|howdy|good morning|good afternoon|good evening)( nova)?")) return Intent.GREETING;
        if (x.equals("what do you think")) return Intent.SMALL_TALK;
        if (containsAny(x,
                "how are you", "how are things", "how s it going", "how is it going", "you good", "are you okay",
                "how was your day", "how has your day been", "thanks", "thank you", "cheers", "nice", "awesome", "great", "good job", "well done",
                "who are you", "what are you", "what s your name", "what is your name", "tell me about yourself",
                "what are you doing", "are you there", "good night", "bye", "goodbye", "see you",
                "tell me a joke", "joke", "i m tired", "im tired", "i am tired", "i m bored", "im bored", "i am bored")) return Intent.SMALL_TALK;
        if (containsAny(x, "what can you do", "capabilities", "help me use nova", "how can you help", "show me what you can do", "what features do you have")) return Intent.CAPABILITIES;
        if (containsAny(x,
                "daily brief", "morning brief", "evening brief", "brief me", "give me the rundown", "give me a rundown",
                "what s happening today", "what is happening today", "what s going on today", "what is going on today", "morley brief")) return Intent.DAILY_BRIEF;
        if (containsAny(x,
                "knowledge base", "nova knowledge", "what do you know", "show your knowledge", "knowledge summary",
                "what knowledge do you have", "what have you stored")) return Intent.KNOWLEDGE;
        if (containsAny(x,
                "what needs my attention", "what needs attention", "needs attention", "need my attention",
                "what should i look at", "what should i check", "highest priority", "urgent items", "anything urgent",
                "anything wrong", "what s wrong", "what is wrong", "any problems", "any blockers", "risks right now", "what should i fix first")) return Intent.ATTENTION;
        if (containsAny(x, "guardian", "approval", "protected repair", "incident", "risk signal", "repair queue")) return Intent.GUARDIAN;
        if (containsAny(x, "support", "ticket", "sla", "customer issue", "helpdesk", "unassigned ticket")) return Intent.SUPPORT;
        if (containsAny(x, "inventory", "stock", "in stock", "available items", "stock health", "inventory health")) return Intent.INVENTORY;
        if (containsAny(x,
                "catalogue", "catalog", "device list", "model number", "storage options", "release year", "catalogue gap",
                "missing device info", "missing specs", "duplicate device", "bad model number")) return Intent.CATALOGUE;
        if (containsAny(x, "learn", "learning", "outcome", "pattern", "accuracy", "forecast error", "improve", "what did you learn", "learning health")) return Intent.LEARNING;
        if (containsAny(x, "release", "deployment", "version", "apk", "ota", "what changed", "what s new", "latest build", "latest update")) return Intent.RELEASES;
        if (containsAny(x,
                "sales", "profit", "performance", "revenue", "margin", "valuation", "business overview", "business health",
                "how is morley doing", "how s morley doing", "morley status", "business status", "overall performance")) return Intent.PERFORMANCE;
        return Intent.UNKNOWN;
    }

    static boolean isFollowUp(String text) {
        String x = normalise(text);
        if (x.isEmpty() || x.length() >= 120) return false;
        return x.equals("why") || x.equals("how") || x.equals("which") || x.equals("tell me more") ||
                x.equals("more") || x.equals("what else") || x.equals("what about that") || x.equals("really") ||
                x.equals("and that") || x.equals("go on") || x.equals("keep going") || x.equals("explain") ||
                x.startsWith("why ") || x.startsWith("which ") || x.startsWith("what about ") || x.startsWith("how about ") || x.startsWith("and ") ||
                x.startsWith("tell me more") || x.startsWith("go deeper") || x.startsWith("explain that") || x.startsWith("show me more");
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
