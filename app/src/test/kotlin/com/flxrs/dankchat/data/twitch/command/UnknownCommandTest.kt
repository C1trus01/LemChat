package com.flxrs.dankchat.data.twitch.command

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class UnknownCommandTest {
    @Test
    fun `regular messages and allowed commands are not unknown commands`() {
        val messages = listOf(
            "hello",
            "/me hello",
            ".me hello",
            "/ME hello",
            "/me",
            ".me",
            "/ hello",
            ". hello",
            ". .hello",
            "/ .hello",
            ". /hello",
            ".",
            "..",
            "...",
            "....",
            "",
            "foo",
            "a",
            "!",
            ". .",
            ". ..",
            ".. ..",
            ".. .",
            "/ /",
            "/ .",
            ". /",
            ". ./",
            ".. /",
            ".. me",
            ". me",
        )

        messages.forEach { message ->
            assertFalse(TwitchCommandRepository.isUnknownCommand(message), "expected \"$message\" to be allowed")
        }
    }

    @Test
    fun `unknown command messages are detected`() {
        val messages = listOf(
            "/hello",
            ".hello",
            "/mehello",
            ".mehello",
            "/mehello world",
            ".mehello world",
            "/badcommand hello",
            ".badcommand hello",
            "/@badcommand hello",
            ".@badcommand hello",
            "/bann username ban reason",
            "/bann username",
            "//",
            "./",
            "./me",
            "./w",
            "/.",
            "/.me",
            "/.w",
            "/,me",
        )

        messages.forEach { message ->
            assertTrue(TwitchCommandRepository.isUnknownCommand(message), "expected \"$message\" to be detected")
        }
    }
}
