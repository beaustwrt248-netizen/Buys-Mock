package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminModePolicyTest {
    @Test fun adminCanEnter() = assertTrue(AdminModePolicy.canEnter("admin"))
    @Test fun managerCanEnter() = assertTrue(AdminModePolicy.canEnter("manager"))
    @Test fun normalisationIsSafe() = assertTrue(AdminModePolicy.canEnter(" Manager "))
    @Test fun staffCannotEnter() = assertFalse(AdminModePolicy.canEnter("staff"))
    @Test fun userCannotEnter() = assertFalse(AdminModePolicy.canEnter("user"))
    @Test fun unknownCannotEnter() = assertFalse(AdminModePolicy.canEnter("owner"))
    @Test fun blankCannotEnter() = assertFalse(AdminModePolicy.canEnter(""))
}
