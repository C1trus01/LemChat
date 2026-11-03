package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.utils.extensions.withoutOAuthPrefix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class AuthDebugSection(
    private val authDataStore: AuthDataStore,
) : DebugSection {
    override val order = 2
    override val baseTitle = "Auth"

    override fun entries(): Flow<DebugSectionSnapshot> = authDataStore.settings.map { auth ->
        val tokenPreview =
            auth.oAuthKey
                ?.withoutOAuthPrefix
                ?.take(8)
                ?.let { "$it..." }
                ?: "N/A"
        DebugSectionSnapshot(
            title = baseTitle,
            entries =
                listOf(
                    DebugEntry("Logged in as", auth.userName ?: "Not logged in"),
                    DebugEntry("User ID", auth.userId ?: "N/A", copyValue = auth.userId),
                    DebugEntry("Token", tokenPreview, copyValue = auth.oAuthKey?.withoutOAuthPrefix),
                ),
        )
    }
}
