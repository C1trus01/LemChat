package com.flxrs.dankchat.data.repo.chat

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class UserNameMatchTest {
    @Test
    fun `names are found as words regardless of case`() {
        assertTrue("hello @LittIeTaco, how are you".containsUserName("littietaco"))
        assertTrue("littietaco: hi".containsUserName("littietaco"))
        assertTrue("hi littietaco".containsUserName("littietaco"))
    }

    @Test
    fun `names inside of other words are not found`() {
        assertFalse("the assistant said hi".containsUserName("as"))
        assertFalse("xforsen_ is here".containsUserName("forsen"))
    }

    @Test
    fun `later matches are found after an earlier one inside of a word`() {
        assertTrue("assistant as".containsUserName("as"))
    }
}
