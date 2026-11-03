package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.IgnoresRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

/** Gates work that must not run before the user's Twitch block list is loaded — i.e. recent message history. */
@Single
class UserBlocksGate(
    private val authDataStore: AuthDataStore,
    private val ignoresRepository: IgnoresRepository,
) {
    private val ready = MutableStateFlow(true)

    fun close() {
        ready.value = false
    }

    fun open() {
        ready.value = true
    }

    suspend fun loadAndOpen() {
        try {
            if (authDataStore.isLoggedIn) {
                ignoresRepository.loadUserBlocks()
            }
        } finally {
            ready.value = true
        }
    }

    suspend fun awaitReady() {
        ready.first { it }
    }
}
