package com.flxrs.dankchat.data.api.seventv

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPresenceRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SevenTVApi(
    private val ktorClient: HttpClient,
) {
    suspend fun getChannelEmotes(channelId: UserId) = ktorClient.get("users/twitch/$channelId")

    suspend fun getUser(sevenTVUserId: String) = ktorClient.get("users/$sevenTVUserId")

    suspend fun getEmoteSet(emoteSetId: String) = ktorClient.get("emote-sets/$emoteSetId")

    suspend fun getGlobalEmotes() = ktorClient.get("emote-sets/global")

    suspend fun sendPresence(
        sevenTVUserId: String,
        request: SevenTVPresenceRequestDto,
    ) = ktorClient.post("users/$sevenTVUserId/presences") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }
}
