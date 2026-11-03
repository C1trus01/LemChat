package com.flxrs.dankchat.data.api.seventv

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.api.recoverNotFoundWith
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteSetDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVFullUserDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPresenceDataDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPresenceRequestDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVUserDto
import com.flxrs.dankchat.data.api.throwApiErrorOnFailure
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class SevenTVApiClient(
    private val sevenTVApi: SevenTVApi,
    private val json: Json,
) {
    suspend fun getSevenTVChannelEmotes(channelId: UserId): Result<SevenTVUserDto?> = runCatching {
        sevenTVApi
            .getChannelEmotes(channelId)
            .throwApiErrorOnFailure(json)
            .body<SevenTVUserDto>()
    }.recoverNotFoundWith(default = null)

    suspend fun getSevenTVUser(sevenTVUserId: String): Result<SevenTVFullUserDto> = runCatching {
        sevenTVApi
            .getUser(sevenTVUserId)
            .throwApiErrorOnFailure(json)
            .body()
    }

    suspend fun getSevenTVEmoteSet(emoteSetId: String): Result<SevenTVEmoteSetDto> = runCatching {
        sevenTVApi
            .getEmoteSet(emoteSetId)
            .throwApiErrorOnFailure(json)
            .body()
    }

    suspend fun getSevenTVGlobalEmotes(): Result<List<SevenTVEmoteDto>> = runCatching {
        sevenTVApi
            .getGlobalEmotes()
            .throwApiErrorOnFailure(json)
            .body<SevenTVEmoteSetDto>()
            .emotes
            .orEmpty()
    }

    suspend fun sendChannelPresence(
        sevenTVUserId: String,
        channelId: UserId,
    ): Result<Unit> = runCatching {
        val request = SevenTVPresenceRequestDto(kind = PRESENCE_KIND_CHANNEL, data = SevenTVPresenceDataDto(id = channelId.value, platform = "TWITCH"))
        sevenTVApi
            .sendPresence(sevenTVUserId, request)
            .throwApiErrorOnFailure(json)
    }

    companion object {
        private const val PRESENCE_KIND_CHANNEL = 1
    }
}
