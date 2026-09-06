package com.buysloans.nova;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NovaCatalogueLiveRefreshContractTest {
    @Test public void catalogueReadsEveryLivePageWithoutCaching() throws Exception {
        String source = new String(Files.readAllBytes(
                Path.of("src/main/java/com/buysloans/nova/NovaApiClient.java")), StandardCharsets.UTF_8);

        assertTrue(source.contains("JSONArray catalogue()"));
        assertTrue(source.contains("return getAllPages("));
        assertTrue(source.contains("order=id.asc"));
        assertTrue(source.contains("offset += page.length()"));
        assertTrue(source.contains("if (page.length() < pageSize) return all"));
        assertTrue(source.contains("connection.setUseCaches(false)"));
        assertTrue(source.contains("Cache-Control\", \"no-cache, no-store"));
        assertFalse(source.contains("release_year.desc.nullslast&limit=1000"));
    }
}
