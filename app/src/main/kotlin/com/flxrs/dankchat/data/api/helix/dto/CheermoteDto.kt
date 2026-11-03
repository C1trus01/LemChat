package com.flxrs.dankchat.data.api.helix.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CheermoteSetDto(
    val prefix: String,
    val tiers: List<CheermoteTierDto>,
    val type: String,
    val order: Int,
)

@Keep
@Serializable
data class CheermoteTierDto(
    @SerialName("min_bits") val minBits: Int,
    val id: String,
    val color: String,
    val images: CheermoteTierImagesDto,
)

@Keep
@Serializable
data class CheermoteTierImagesDto(
    val dark: CheermoteThemeImagesDto,
    val light: CheermoteThemeImagesDto,
)

@Keep
@Serializable
data class CheermoteThemeImagesDto(
    val animated: Map<String, String>,
    val static: Map<String, String>,
)
