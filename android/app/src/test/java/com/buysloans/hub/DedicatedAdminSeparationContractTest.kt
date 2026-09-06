package com.buysloans.hub

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DedicatedAdminSeparationContractTest {
    @Test
    fun dedicatedAdminProductsRemainPresent() {
        val projectRoot = File("../..").canonicalFile
        assertTrue(File(projectRoot, "admin/index.html").isFile)
        assertTrue(File(projectRoot, "android/adminapp").isDirectory)
    }
}
