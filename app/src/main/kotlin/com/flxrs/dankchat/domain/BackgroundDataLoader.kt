package com.flxrs.dankchat.domain

import com.flxrs.dankchat.di.DispatchersProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger("BackgroundDataLoader")

/**
 * Runs best-effort loads on a scope that no loading state depends on, so unstable APIs can never
 * gate the UI. A load is skipped while one with the same tag is still in flight.
 */
@Single
class BackgroundDataLoader(
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val jobs = ConcurrentHashMap<String, Job>()

    fun load(
        tag: String,
        block: suspend () -> Unit,
    ) {
        jobs.compute(tag) { _, existing ->
            when {
                existing?.isActive == true -> existing

                else -> scope.launch {
                    try {
                        block()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        logger.warn(t) { "Background load '$tag' failed" }
                    }
                }
            }
        }
    }
}
