package com.flxrs.dankchat.data.repo.crash

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ProcessExitTraceTest {
    @Test
    fun `freeze traces start at the main thread`() {
        val trace = "----- pid 1 -----\nCmd line: app\n\n\"main\" prio=5 tid=1 Blocked\n  at com.flxrs.Foo.bar(Foo.kt:1)\n"

        assertEquals("\"main\" prio=5 tid=1 Blocked\n  at com.flxrs.Foo.bar(Foo.kt:1)\n", mainThreadFirst(trace))
    }

    @Test
    fun `freeze traces without a main thread stay as they are`() {
        assertEquals("no threads", mainThreadFirst("no threads"))
    }

    @Test
    fun `freeze traces are limited in size`() {
        assertEquals(30_000, mainThreadFirst("\"main\"" + "x".repeat(100_000)).length)
    }

    @Test
    fun `native traces only keep signals, libraries and functions`() {
        val tombstone =
            byteArrayOf(0, 1, 2) +
                "SIGSEGV".toByteArray() +
                byteArrayOf(0) +
                "/system/lib64/libhwui.so".toByteArray() +
                byteArrayOf(7) +
                "_ZN7android10uirenderer4drawEv".toByteArray() +
                byteArrayOf(0) +
                "oauth:secrettoken".toByteArray() +
                byteArrayOf(0, 9) +
                "hello chat message".toByteArray()

        val summary = nativeTraceSummary(tombstone)

        assertTrue("SIGSEGV" in summary)
        assertTrue("/system/lib64/libhwui.so" in summary)
        assertTrue("_ZN7android10uirenderer4drawEv" in summary)
        assertFalse("secrettoken" in summary)
        assertFalse("chat" in summary)
    }
}
