package com.flxrs.dankchat.data.api.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.parameters

class AuthApi(
    private val ktorClient: HttpClient,
) {
    suspend fun validateUser(token: String) = ktorClient.get("validate") {
        bearerAuth(token)
    }

    suspend fun revokeToken(
        token: String,
        clientId: String,
    ) = ktorClient.submitForm(
        url = "revoke",
        formParameters =
            parameters {
                append("client_id", clientId)
                append("token", token)
            },
    )
}
