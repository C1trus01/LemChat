package com.flxrs.dankchat.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.api.auth.AuthApiClient
import com.flxrs.dankchat.data.api.auth.dto.ValidateDto
import com.flxrs.dankchat.data.auth.AuthDataStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

private val logger = KotlinLogging.logger("LoginViewModel")

@KoinViewModel
class LoginViewModel(
    private val authApiClient: AuthApiClient,
    private val authDataStore: AuthDataStore,
) : ViewModel() {
    data class TokenParseEvent(
        val successful: Boolean,
    )

    private val eventChannel = Channel<TokenParseEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val loginUrl = AuthApiClient.LOGIN_URL

    fun parseToken(fragment: String) = viewModelScope.launch {
        if (!fragment.startsWith("access_token")) {
            eventChannel.send(TokenParseEvent(successful = false))
            return@launch
        }

        val token =
            fragment
                .substringAfter("access_token=")
                .substringBefore("&scope=")

        val result =
            authApiClient.validateUser(token).fold(
                onSuccess = { saveLoginDetails(token, it) },
                onFailure = {
                    logger.error { "Failed to validate token: ${it.message}" }
                    TokenParseEvent(successful = false)
                },
            )
        eventChannel.send(result)
    }

    private suspend fun saveLoginDetails(
        oAuth: String,
        validateDto: ValidateDto,
    ): TokenParseEvent {
        authDataStore.login(
            oAuthKey = oAuth,
            userName = validateDto.login.value.lowercase(),
            userId = validateDto.userId.value,
            clientId = validateDto.clientId,
        )
        return TokenParseEvent(successful = true)
    }
}
