package com.flxrs.dankchat.data.api.github

import io.ktor.client.HttpClient
import io.ktor.client.request.get

class GitHubApi(
    private val ktorClient: HttpClient,
) {
    suspend fun getLatestRelease(repository: String) = ktorClient.get("repos/$repository/releases/latest")
}
