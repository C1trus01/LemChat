package com.flxrs.dankchat.data.api.seventv.eventapi

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteDto
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.SevenTVPaintDto

sealed interface SevenTVEventMessage {
    data class EmoteSetUpdated(
        val emoteSetId: String,
        val actorName: DisplayName,
        val added: List<SevenTVEmoteDto>,
        val removed: List<RemovedEmote>,
        val updated: List<UpdatedEmote>,
    ) : SevenTVEventMessage {
        data class UpdatedEmote(
            val id: String,
            val name: String,
            val oldName: String,
        )

        data class RemovedEmote(
            val id: String,
            val name: String,
        )
    }

    data class UserUpdated(
        val actorName: DisplayName,
        val connectionIndex: Int,
        val emoteSetId: String,
        val oldEmoteSetId: String,
    ) : SevenTVEventMessage

    data class PersonalEmoteSetCreated(
        val emoteSetId: String,
    ) : SevenTVEventMessage

    data class PersonalEmoteSetUpdated(
        val emoteSetId: String,
        val added: List<SevenTVEmoteDto>,
        val removed: List<EmoteSetUpdated.RemovedEmote>,
        val updated: List<EmoteSetUpdated.UpdatedEmote>,
    ) : SevenTVEventMessage

    data class PersonalEmoteSetAssigned(
        val emoteSetId: String,
        val userIds: List<UserId>,
    ) : SevenTVEventMessage

    data class PersonalEmoteSetRemoved(
        val emoteSetId: String,
        val userIds: List<UserId>,
    ) : SevenTVEventMessage

    data class BadgeCreated(
        val badgeId: String,
        val title: String?,
        val url: String,
    ) : SevenTVEventMessage

    data class BadgeAssigned(
        val badgeId: String,
        val userIds: List<UserId>,
    ) : SevenTVEventMessage

    data class BadgeRemoved(
        val badgeId: String,
        val userIds: List<UserId>,
    ) : SevenTVEventMessage

    data class PaintCreated(
        val paint: SevenTVPaintDto,
    ) : SevenTVEventMessage

    data class PaintAssigned(
        val paintId: String,
        val userIds: List<UserId>,
        val userNames: List<UserName>,
    ) : SevenTVEventMessage

    data class PaintRemoved(
        val paintId: String,
        val userIds: List<UserId>,
        val userNames: List<UserName>,
    ) : SevenTVEventMessage
}
