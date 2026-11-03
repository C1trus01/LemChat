package com.flxrs.dankchat.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthAccount(
    val userId: String,
    val userName: String,
    val displayName: String? = null,
    val oAuthKey: String,
    val clientId: String = AuthSettings.DEFAULT_CLIENT_ID,
)

@Serializable
data class AuthSettings(
    val oAuthKey: String? = null,
    val userName: String? = null,
    val displayName: String? = null,
    val userId: String? = null,
    val clientId: String = DEFAULT_CLIENT_ID,
    val isLoggedIn: Boolean = false,
    val accounts: List<AuthAccount> = emptyList(),
) {
    private val activeAccount: AuthAccount?
        get() {
            if (!isLoggedIn) return null
            return AuthAccount(
                userId = userId ?: return null,
                userName = userName ?: return null,
                displayName = displayName,
                oAuthKey = oAuthKey ?: return null,
                clientId = clientId,
            )
        }

    // The active fields are the source of truth, so a login from before accounts existed or a refreshed token is always included
    val allAccounts: List<AuthAccount>
        get() {
            val active = activeAccount ?: return accounts
            return when {
                accounts.any { it.userId == active.userId } -> accounts.map { if (it.userId == active.userId) active else it }
                else -> accounts + active
            }
        }

    companion object {
        const val DEFAULT_CLIENT_ID = "zaanf2d77rgstxbptqse2wdbhi8d9n"
    }
}
