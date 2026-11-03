package com.flxrs.dankchat.data.api.seventv.eventapi.dto

enum class SubscriptionType(
    val type: String,
) {
    UserUpdates(type = "user.update"),
    EmoteSetUpdates(type = "emote_set.update"),
    CosmeticCreate(type = "cosmetic.create"),
    EntitlementCreate(type = "entitlement.create"),
    EntitlementDelete(type = "entitlement.delete"),
    AnyEmoteSet(type = "emote_set.*"),
    ;

    companion object {
        val ChannelCosmetics = listOf(CosmeticCreate, EntitlementCreate, EntitlementDelete, AnyEmoteSet)
    }
}
