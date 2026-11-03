package com.flxrs.dankchat.data.api.seventv.eventapi

import com.flxrs.dankchat.data.api.seventv.eventapi.dto.CosmeticCreateDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.DataMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.DispatchMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EmoteSetCreateDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EmoteSetDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EntitlementCreateDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.SevenTVPaintDto
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.SubscribeRequest
import com.flxrs.dankchat.data.repo.emote.SevenTVPaint
import com.flxrs.dankchat.data.repo.emote.toPaint
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The payloads mirror what events.7tv.io sends for channel subscriptions, shortened to the relevant fields
internal class SevenTVCosmeticEventsTest {
    private val json =
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        }

    private inline fun <reified T> decodeDispatch(text: String): T {
        val message = assertIs<DispatchMessage>(json.decodeFromString<DataMessage>(text))
        return assertIs<T>(message.d)
    }

    @Test
    fun `channel subscriptions only send the channel condition`() {
        val requests = SubscribeRequest.channelCosmetics("22484632").map { json.encodeToString(it) }

        assertEquals(4, requests.size)
        assertEquals(
            """{"op":35,"d":{"type":"cosmetic.create","condition":{"ctx":"channel","platform":"TWITCH","id":"22484632"}}}""",
            requests.first(),
        )
    }

    @Test
    fun `object subscriptions only send the object id`() {
        assertEquals(
            """{"op":35,"d":{"type":"emote_set.update","condition":{"object_id":"set"}}}""",
            json.encodeToString(SubscribeRequest.emoteSetUpdates("set")),
        )
    }

    @Test
    fun `personal and commercial emote sets are recognized by their flags`() {
        fun created(flags: Int) = decodeDispatch<EmoteSetCreateDispatchData>(
            """{"d":{"type":"emote_set.create","body":{"id":"set","kind":3,"object":{"id":"set","name":"name","flags":$flags,"capacity":5}}},"op":0,"t":1,"s":1}""",
        ).body.emoteSet

        assertTrue(created(flags = 4).isPersonalOrCommercial)
        assertTrue(created(flags = 11).isPersonalOrCommercial)
        assertFalse(created(flags = 0).isPersonalOrCommercial)
        assertFalse(created(flags = 3).isPersonalOrCommercial)
    }

    @Test
    fun `emote set updates of channel subscriptions come without an actor`() {
        val update = decodeDispatch<EmoteSetDispatchData>(
            """{"d":{"type":"emote_set.update","body":{"id":"set","kind":3,"pushed":[{"key":"emotes","index":0,"type":"object","value":{"id":"emote","name":"nnysBop","flags":0,"timestamp":1,"actor_id":"a","data":{"id":"emote","name":"nnysBop","flags":1,"lifecycle":3,"state":["PERSONAL","LISTED"],"listed":true,"animated":true,"owner":{"id":"a","username":"a","display_name":"a"},"host":{"url":"//cdn.7tv.app/emote/emote","files":[{"name":"4x.webp","format":"WEBP"}]}}}}]}},"op":0,"t":1,"s":1}""",
        ).body

        assertNull(update.actor)
        assertEquals(listOf("PERSONAL", "LISTED"), update.pushed?.single()?.value?.data?.state)
    }

    @Test
    fun `entitlements resolve to the twitch connections of the user`() {
        val entitlement = decodeDispatch<EntitlementCreateDispatchData>(
            """{"d":{"type":"entitlement.create","body":{"id":"0","kind":10,"object":{"id":"0","kind":"PAINT","ref_id":"paint","user":{"id":"u","username":"name","style":{"paint_id":"paint"},"connections":[{"id":"1","platform":"KICK","username":"name"},{"id":"704178938","platform":"TWITCH","username":"name","emote_set_id":null}]}}}},"op":0,"t":1,"s":1}""",
        ).body.entitlement

        assertEquals("PAINT", entitlement.kind)
        assertEquals("paint", entitlement.refId)
        assertEquals(listOf("704178938".toUserId()), entitlement.twitchUserIds)
        assertEquals(listOf("name".toUserName()), entitlement.twitchUserNames)
    }

    @Test
    fun `entitlement user names are lowercase like the names of mentions`() {
        val entitlement = decodeDispatch<EntitlementCreateDispatchData>(
            """{"d":{"type":"entitlement.create","body":{"id":"0","object":{"kind":"PAINT","ref_id":"paint","user":{"connections":[{"id":"1","platform":"TWITCH","username":"LittIeTaco"},{"id":"2","platform":"TWITCH"}]}}}},"op":0,"t":1,"s":1}""",
        ).body.entitlement

        assertEquals(listOf("littietaco".toUserName()), entitlement.twitchUserNames)
    }

    @Test
    fun `paints are parsed from their cosmetic data`() {
        val cosmetic = decodeDispatch<CosmeticCreateDispatchData>(
            """{"d":{"type":"cosmetic.create","body":{"id":"p","kind":10,"object":{"id":"p","kind":"PAINT","data":{"id":"p","name":"Candy Cane","color":null,"gradients":[],"shadows":[{"x_offset":-1.0,"y_offset":0.0,"radius":0.7,"color":102},{"x_offset":1.0,"y_offset":0.0,"radius":0.7,"color":102}],"text":null,"function":"LINEAR_GRADIENT","repeat":true,"angle":45,"shape":"circle","image_url":"","stops":[{"at":0.1,"color":-757935361},{"at":0.3,"color":-10197761}]}}}},"op":0,"t":1,"s":1}""",
        ).body.cosmetic

        val paint = assertNotNull(json.decodeFromJsonElement<SevenTVPaintDto>(cosmetic.data).toPaint())

        val fill = assertIs<SevenTVPaint.Fill.Linear>(paint.fill)
        assertEquals(45f, fill.angle)
        assertTrue(fill.repeat)
        assertEquals(listOf(0.1f, 0.3f), fill.stops.map { it.position })
        // 0xD2D2D2FF as RGBA
        assertEquals(0xFFD2D2D2.toInt(), fill.stops.first().color)
        // Opposite offsets average to a centered shadow that still reaches as far
        val shadow = assertNotNull(paint.shadow)
        assertEquals(0f, shadow.xOffset)
        assertEquals(1.7f, shadow.radius, 0.001f)
        assertEquals(0x66000000, shadow.color)
    }

    @Test
    fun `image paints need an image and unknown functions are dropped`() {
        val image = """{"id":"p","function":"URL","image_url":"https://cdn.7tv.app/paint/p/1x.webp","stops":[]}"""
        val blankImage = """{"id":"p","function":"URL","image_url":""}"""
        val unknown = """{"id":"p","function":"SOMETHING_NEW"}"""

        val fill = assertIs<SevenTVPaint.Fill.Image>(json.decodeFromString<SevenTVPaintDto>(image).toPaint()?.fill)
        assertEquals("https://cdn.7tv.app/paint/p/1x.webp", fill.url)
        assertNull(json.decodeFromString<SevenTVPaintDto>(blankImage).toPaint())
        assertNull(json.decodeFromString<SevenTVPaintDto>(unknown).toPaint())
    }
}
