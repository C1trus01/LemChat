package com.flxrs.dankchat.domain

import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class BackgroundDataLoaderTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchersProvider =
        object : DispatchersProvider {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val immediate: CoroutineDispatcher = testDispatcher
        }

    private val loader = BackgroundDataLoader(dispatchersProvider)

    @Test
    fun `load executes the block`() = runTest(testDispatcher) {
        var executed = false

        loader.load("tag") { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `load with the same tag is skipped while one is in flight`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        var executions = 0

        loader.load("tag") {
            executions++
            gate.await()
        }
        loader.load("tag") { executions++ }
        assertEquals(1, executions)

        gate.complete(Unit)
        loader.load("tag") { executions++ }
        assertEquals(2, executions)
    }

    @Test
    fun `loads with different tags run independently`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        var otherExecuted = false

        loader.load("first") { gate.await() }
        loader.load("second") { otherExecuted = true }

        assertTrue(otherExecuted)
        gate.complete(Unit)
    }

    @Test
    fun `failing load is contained and the tag can load again`() = runTest(testDispatcher) {
        var executed = false

        loader.load("tag") { error("supinic.com is down") }
        loader.load("tag") { executed = true }

        assertTrue(executed)
    }
}
