package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Test

class UserSearchPolicyTest {
    private val users = listOf(
        UserAccessPresentation("1", "Alice Manager", "manager", true, false, false, false, false, null),
        UserAccessPresentation("2", "Bob Staff", "staff", false, false, false, false, false, null),
        UserAccessPresentation("3", "Primary Admin", "admin", true, true, false, false, false, null)
    )

    @Test fun blankQueryReturnsAllRows() {
        assertEquals(3, filterUserAccessRows(users, "  ").size)
    }

    @Test fun matchesNameRoleAndStateCaseInsensitively() {
        assertEquals(listOf("Alice Manager"), filterUserAccessRows(users, "ALICE").map { it.displayName })
        assertEquals(listOf("Bob Staff"), filterUserAccessRows(users, "staff").map { it.displayName })
        assertEquals(listOf("Bob Staff"), filterUserAccessRows(users, "disabled").map { it.displayName })
        assertEquals(listOf("Primary Admin"), filterUserAccessRows(users, "signed in").map { it.displayName })
    }

    @Test fun doesNotSearchInternalUserIds() {
        assertEquals(emptyList<UserAccessPresentation>(), filterUserAccessRows(users, "2"))
    }
}
