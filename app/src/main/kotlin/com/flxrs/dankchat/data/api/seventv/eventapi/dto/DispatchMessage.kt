package com.flxrs.dankchat.data.api.seventv.eventapi.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteHostDto
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject

@Serializable
@SerialName("0")
data class DispatchMessage(
    override val d: DispatchData,
) : DataMessage

@Serializable
@JsonClassDiscriminator(discriminator = "type")
sealed interface DispatchData : Data {
    val body: ChangeMapData
}

interface ChangeMapData {
    val id: String
}

@Serializable
data class Actor(
    @SerialName("display_name") val displayName: DisplayName,
)

@Serializable
@SerialName("emote_set.update")
data class EmoteSetDispatchData(
    override val body: EmoteSetChangeMapData,
) : DispatchData

@Serializable
data class EmoteSetChangeMapData(
    override val id: String,
    // Updates of personal sets, sent through channel subscriptions, are not attributed to an actor
    val actor: Actor?,
    val pushed: List<EmoteChangeField>?,
    val pulled: List<EmoteChangeField>?,
    val updated: List<EmoteChangeField>?,
) : ChangeMapData

@Serializable
@JsonClassDiscriminator("key")
sealed interface ChangeField

@Serializable
@SerialName("emotes")
data class EmoteChangeField(
    val value: SevenTVEmoteDto?,
    @SerialName("old_value") val oldValue: SevenTVEmoteDto?,
) : ChangeField

@Serializable
@SerialName("user.update")
data class UserDispatchData(
    override val body: UserChangeMapData,
) : DispatchData

@Serializable
data class UserChangeMapData(
    override val id: String,
    val actor: Actor,
    val updated: List<UserChangeFields>?,
) : ChangeMapData

@Serializable
@SerialName("connections")
data class UserChangeFields(
    val value: List<UserChangeField>?,
    val index: Int,
) : ChangeField

@Serializable
sealed interface UserChangeField : ChangeField

@Serializable
@SerialName("emote_set")
data class EmoteSetChangeField(
    val value: EmoteSet,
    @SerialName("old_value") val oldValue: EmoteSet,
) : UserChangeField

@Keep
@Serializable
@SerialName("emote_set_id")
data object EmoteSetIdChangeField : UserChangeField

@Serializable
data class EmoteSet(
    val id: String,
)

@Serializable
@SerialName("emote_set.create")
data class EmoteSetCreateDispatchData(
    override val body: EmoteSetCreateData,
) : DispatchData

@Serializable
data class EmoteSetCreateData(
    override val id: String,
    @SerialName("object") val emoteSet: EmoteSetObject,
) : ChangeMapData

@Serializable
data class EmoteSetObject(
    val id: String,
    val flags: Long = 0,
) {
    val isPersonalOrCommercial get() = flags and (PERSONAL_FLAG or COMMERCIAL_FLAG) != 0L

    companion object {
        private const val PERSONAL_FLAG = 1L shl 2
        private const val COMMERCIAL_FLAG = 1L shl 3
    }
}

@Serializable
@SerialName("cosmetic.create")
data class CosmeticCreateDispatchData(
    override val body: CosmeticCreateData,
) : DispatchData

@Serializable
data class CosmeticCreateData(
    override val id: String,
    @SerialName("object") val cosmetic: CosmeticObject,
) : ChangeMapData

// The shape of the data depends on the kind, badges are not paints
@Serializable
data class CosmeticObject(
    val kind: String,
    val data: JsonObject,
)

@Serializable
data class SevenTVBadgeDto(
    val id: String,
    val tooltip: String? = null,
    val host: SevenTVEmoteHostDto,
) {
    // A badge slot is 18dp, so 3x is what a typical phone draws
    val imageUrl: String?
        get() {
            val file = host.files.firstOrNull { it.name.startsWith("3x") } ?: host.files.lastOrNull() ?: return null
            return "https:${host.url}/${file.name}"
        }
}

@Serializable
data class SevenTVPaintDto(
    val id: String,
    val function: String = "",
    val repeat: Boolean = false,
    val angle: Float = 0f,
    @SerialName("image_url") val imageUrl: String? = null,
    val stops: List<SevenTVPaintStopDto> = emptyList(),
    val shadows: List<SevenTVPaintShadowDto> = emptyList(),
)

@Serializable
data class SevenTVPaintStopDto(
    val at: Float,
    val color: Long,
)

@Serializable
data class SevenTVPaintShadowDto(
    @SerialName("x_offset") val xOffset: Float,
    @SerialName("y_offset") val yOffset: Float,
    val radius: Float,
    val color: Long,
)

@Serializable
@SerialName("entitlement.create")
data class EntitlementCreateDispatchData(
    override val body: EntitlementData,
) : DispatchData

@Serializable
@SerialName("entitlement.delete")
data class EntitlementDeleteDispatchData(
    override val body: EntitlementData,
) : DispatchData

@Serializable
data class EntitlementData(
    override val id: String,
    @SerialName("object") val entitlement: EntitlementObject,
) : ChangeMapData

@Serializable
data class EntitlementObject(
    val kind: String,
    @SerialName("ref_id") val refId: String,
    val user: EntitlementUser?,
) {
    private val twitchConnections get() = user?.connections.orEmpty().filter { it.platform == "TWITCH" }
    val twitchUserIds get() = twitchConnections.map { it.id.toUserId() }
    val twitchUserNames get() = twitchConnections.mapNotNull { it.username?.toUserName()?.lowercase() }
}

@Serializable
data class EntitlementUser(
    val connections: List<EntitlementConnection> = emptyList(),
)

@Serializable
data class EntitlementConnection(
    val id: String,
    val platform: String,
    val username: String? = null,
)
