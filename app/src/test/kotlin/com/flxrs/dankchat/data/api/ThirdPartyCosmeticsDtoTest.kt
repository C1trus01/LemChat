package com.flxrs.dankchat.data.api

import com.flxrs.dankchat.data.api.bttv.dto.BTTVUserBadgeDto
import com.flxrs.dankchat.data.api.ffz.dto.FFZBadgeDto
import com.flxrs.dankchat.data.api.ffz.dto.FFZBadgesDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVFullUserDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVUserEmoteSetDto
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.SevenTVBadgeDto
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ThirdPartyCosmeticsDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val subscriberRole = "01F37R3RFR0000K96678WEQT01"

    @Test
    fun `7tv badge from a real event uses the 3x image`() {
        val badge =
            json.decodeFromString<SevenTVBadgeDto>(
                """
                {"id":"01KV1CC9N5R1RG1TGHK0T42WZN","name":"Australia","tag":"","tooltip":"Australia",
                 "host":{"url":"//cdn.7tv.app/badge/01KV1CC9N5R1RG1TGHK0T42WZN","files":[
                   {"name":"1x.webp","static_name":"1x_static.webp","width":18,"height":18,"frame_count":60,"size":19146,"format":"WEBP"},
                   {"name":"3x.webp","static_name":"3x_static.webp","width":54,"height":54,"frame_count":60,"size":19146,"format":"WEBP"},
                   {"name":"4x.webp","static_name":"4x_static.webp","width":72,"height":72,"frame_count":60,"size":19146,"format":"WEBP"}
                 ]}}
                """.trimIndent(),
            )

        assertEquals("Australia", badge.tooltip)
        assertEquals("https://cdn.7tv.app/badge/01KV1CC9N5R1RG1TGHK0T42WZN/3x.webp", badge.imageUrl)
    }

    @Test
    fun `7tv badge without a 3x image falls back to the largest one`() {
        val badge =
            json.decodeFromString<SevenTVBadgeDto>(
                """{"id":"a","host":{"url":"//cdn.7tv.app/badge/a","files":[{"name":"1x.webp","format":"WEBP"},{"name":"2x.webp","format":"WEBP"}]}}""",
            )

        assertEquals("https://cdn.7tv.app/badge/a/2x.webp", badge.imageUrl)
    }

    @Test
    fun `7tv badge without images has no url`() {
        val badge = json.decodeFromString<SevenTVBadgeDto>("""{"id":"a","host":{"url":"//cdn.7tv.app/badge/a","files":[]}}""")

        assertNull(badge.imageUrl)
    }

    @Test
    fun `ffz badges from a real response keep users per badge`() {
        val dto =
            json.decodeFromString<FFZBadgesDto>(
                """
                {"badges":[{"id":2,"name":"bot","title":"Bot","slot":62,"replaces":"bot","color":"#595959","image":"x",
                  "urls":{"1":"https://cdn.frankerfacez.com/badge/2/1","2":"https://cdn.frankerfacez.com/badge/2/2","4":"https://cdn.frankerfacez.com/badge/2/4"}}],
                 "users":{"2":[1564983,100135110]}}
                """.trimIndent(),
            )

        assertEquals("https://cdn.frankerfacez.com/badge/2/4", dto.badges.single().imageUrl)
        assertEquals(0xFF595959.toInt(), dto.badges.single().backgroundColor)
        assertEquals(listOf(1564983L, 100135110L), dto.users["2"])
    }

    @Test
    fun `ffz badge color that can't be read means no background`() {
        assertNull(FFZBadgeDto(id = 1).backgroundColor)
        assertNull(FFZBadgeDto(id = 1, color = "transparent").backgroundColor)
        assertNull(FFZBadgeDto(id = 1, color = "#12345").backgroundColor)
    }

    @Test
    fun `bttv badge from a real response keeps user and image`() {
        val dto =
            json.decodeFromString<List<BTTVUserBadgeDto>>(
                """[{"id":"x","name":"n","displayName":"N","providerId":"12345","badge":{"type":1,"description":"NightDev Developer","svg":"https://cdn.betterttv.net/badges/developer.svg"}}]""",
            )

        assertEquals("12345", dto.single().providerId)
        assertEquals("https://cdn.betterttv.net/badges/developer.svg", dto.single().badge.svg)
    }

    @Test
    fun `a subscriber with a personal set can use it`() {
        val user =
            SevenTVFullUserDto(
                roles = listOf("other", subscriberRole),
                emoteSets = listOf(SevenTVUserEmoteSetDto("normal", flags = 0), SevenTVUserEmoteSetDto("personal", flags = 4)),
            )

        assertEquals("personal", user.activePersonalEmoteSetId)
    }

    @Test
    fun `the personal flag counts next to other flags`() {
        val user = SevenTVFullUserDto(roles = listOf(subscriberRole), emoteSets = listOf(SevenTVUserEmoteSetDto("personal", flags = 4 or 1)))

        assertEquals("personal", user.activePersonalEmoteSetId)
    }

    @Test
    fun `a personal set is of no use once the subscription ended`() {
        val user = SevenTVFullUserDto(roles = listOf("other"), emoteSets = listOf(SevenTVUserEmoteSetDto("personal", flags = 4)))

        assertNull(user.activePersonalEmoteSetId)
    }

    @Test
    fun `a subscriber without a personal set has nothing to show`() {
        val user = SevenTVFullUserDto(roles = listOf(subscriberRole), emoteSets = listOf(SevenTVUserEmoteSetDto("normal", flags = 0)))

        assertNull(user.activePersonalEmoteSetId)
    }
}
