package com.flxrs.dankchat.preferences

import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.auth.AuthSettings
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserName
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DankChatPreferenceStoreTest {
    private val authSettings = MutableStateFlow(AuthSettings(userName = "first_user", displayName = "First_User", isLoggedIn = true))

    // Only the emitted settings are stubbed, reading the separately cached account would fail the test
    private val authDataStore: AuthDataStore = mockk { every { settings } returns authSettings }

    private val store =
        DankChatPreferenceStore(
            context = mockk(relaxed = true),
            json = Json,
            appearanceSettingsDataStore = mockk(relaxed = true),
            authDataStore = authDataStore,
        )

    @Test
    fun `current user follows an account switch`() = runTest {
        assertEquals("first_user".toUserName() to "First_User".toDisplayName(), store.currentUserAndDisplayFlow.first())

        authSettings.value = AuthSettings(userName = "second_user", displayName = "second_user", isLoggedIn = true)

        assertEquals("second_user".toUserName() to "second_user".toDisplayName(), store.currentUserAndDisplayFlow.first())
    }
}
