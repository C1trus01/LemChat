package com.flxrs.dankchat.data.api.github.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Keep
@Serializable
data class GitHubAssetDto(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
)
