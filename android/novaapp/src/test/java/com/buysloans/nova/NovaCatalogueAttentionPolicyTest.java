package com.buysloans.nova;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NovaCatalogueAttentionPolicyTest {
    @Test
    public void requiresStorageOnlyForStorageBearingCategories() {
        assertTrue(NovaAssistantEngine.categoryRequiresStorage("mobile_phone"));
        assertTrue(NovaAssistantEngine.categoryRequiresStorage("tablet"));
        assertTrue(NovaAssistantEngine.categoryRequiresStorage("laptop"));
        assertTrue(NovaAssistantEngine.categoryRequiresStorage("desktop"));
        assertTrue(NovaAssistantEngine.categoryRequiresStorage("console"));
        assertFalse(NovaAssistantEngine.categoryRequiresStorage("wearable"));
        assertFalse(NovaAssistantEngine.categoryRequiresStorage(""));
        assertFalse(NovaAssistantEngine.categoryRequiresStorage(null));
    }
}
