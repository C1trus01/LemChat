package com.flxrs.dankchat.data.api.github

import com.flxrs.dankchat.data.api.github.dto.GitHubReleaseDto
import com.flxrs.dankchat.data.api.recoverNotFoundWith
import com.flxrs.dankchat.data.api.throwApiErrorOnFailure
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class GitHubApiClient(
    private val gitHubApi: GitHubApi,
    private val json: Json,
) {
    /** GitHub answers with not found while a repository has no published release. */
    suspend fun getLatestRelease(repository: String): Result<GitHubReleaseDto?> = runCatching {
        gitHubApi
            .getLatestRelease(repository)
            .throwApiErrorOnFailure(json)
            .body<GitHubReleaseDto>()
    }.recoverNotFoundWith(default = null)
}
