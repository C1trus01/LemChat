package com.flxrs.dankchat.data.twitch.message

import com.flxrs.dankchat.data.irc.IrcMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("MaxLineLength")
internal class UserNoticeMessageTest {
    @Test
    fun `parse modiversary user notice`() {
        val msg =
            "@id=090939a1-18a5-4370-9066-b4f56512c320;badges=moderator/1,subscriber/12;tmi-sent-ts=1780134274460;color=#FF7F50;system-msg=has\\sbeen\\sa\\smoderator\\sfor\\s18\\smonths!;login=jcrouzer;user-id=44850681;msg-param-months=18;room-id=11148817;subscriber=1;display-name=JCRouzer;mod=1;flags=;msg-id=modiversary;user-type=mod;badge-info=subscriber/20;emotes=;vip=0 :tmi.twitch.tv USERNOTICE #pajlada :I'm celebrating my 18 month Mod Anniversary!"
        val ircMessage = IrcMessage.parse(msg)
        val notice = UserNoticeMessage.parseUserNotice(ircMessage) { null }

        assertNotNull(notice)
        assertEquals(expected = "JCRouzer has been a moderator for 18 months!", actual = notice.message)
        assertEquals(expected = "I'm celebrating my 18 month Mod Anniversary!", actual = notice.childMessage?.message)
        assertEquals(expected = true, actual = notice.isMilestone)
        assertEquals(expected = false, actual = notice.isSub)
        assertEquals(expected = false, actual = notice.childMessage?.isSub)
        assertEquals(expected = true, actual = notice.childMessage?.isMilestone)
    }

    @Test
    fun `parse announcement user notice`() {
        val msg =
            "@badge-info=subscriber/86;badges=moderator/1,subscriber/3072;color=#2E8B57;display-name=pajbot;emotes=;flags=;id=50f06104-1425-4e61-8d69-19b2e910cc92;login=pajbot;mod=1;msg-id=announcement;msg-param-color=PRIMARY;room-id=11148817;subscriber=1;system-msg=;tmi-sent-ts=1678652624650;user-id=82008718;user-type=mod :tmi.twitch.tv USERNOTICE #pajlada :guh"
        val ircMessage = IrcMessage.parse(msg)
        val notice = UserNoticeMessage.parseUserNotice(ircMessage) { null }

        assertNotNull(notice)
        assertEquals(expected = "Announcement", actual = notice.message)
        assertEquals(expected = "guh", actual = notice.childMessage?.message)
        assertEquals(expected = true, actual = notice.isAnnouncement)
        assertEquals(expected = false, actual = notice.isSub)
        assertEquals(expected = false, actual = notice.isMilestone)
    }

    @Test
    fun `parse resub user notice with user message`() {
        val msg =
            "@badge-info=subscriber/14;badges=subscriber/12,sub-gifter/5;color=#FFB735;display-name=LordGrox;emotes=;flags=;id=b59b84a2-ae2a-437b-81c2-d049f71448d7;login=lordgrox;mod=0;msg-id=resub;msg-param-cumulative-months=14;msg-param-months=0;msg-param-multimonth-duration=0;msg-param-multimonth-tenure=0;msg-param-should-share-streak=0;msg-param-sub-plan-name=Channel\\sSubscription\\s(mandeow);msg-param-sub-plan=1000;msg-param-was-gifted=false;room-id=128856353;subscriber=1;system-msg=LordGrox\\ssubscribed\\sat\\sTier\\s1.\\sThey've\\ssubscribed\\sfor\\s14\\smonths!;tmi-sent-ts=1678873250647;user-id=38584526;user-type= :tmi.twitch.tv USERNOTICE #mande :good day mande and chat, I hope everyone has a banger day."
        val ircMessage = IrcMessage.parse(msg)
        val notice = UserNoticeMessage.parseUserNotice(ircMessage) { null }

        assertNotNull(notice)
        assertEquals(expected = "LordGrox subscribed at Tier 1. They've subscribed for 14 months!", actual = notice.message)
        assertEquals(expected = "good day mande and chat, I hope everyone has a banger day.", actual = notice.childMessage?.message)
        assertEquals(expected = true, actual = notice.isSub)
        assertEquals(expected = false, actual = notice.isAnnouncement)
        assertEquals(expected = false, actual = notice.isMilestone)
    }

    @Test
    fun `parse gift sub user notice without user message`() {
        val msg =
            "@badge-info=subscriber/13;badges=subscriber/12,premium/1;color=#0000FF;display-name=Goop_456789;emotes=;flags=;id=f27c6766-80b3-4eb5-875e-ec892ca4cb3a;login=goop_456789;mod=0;msg-id=subgift;msg-param-gift-months=1;msg-param-months=12;msg-param-recipient-display-name=Zackpanjang;msg-param-recipient-id=412581855;msg-param-recipient-user-name=zackpanjang;msg-param-sender-count=11;msg-param-sub-plan-name=Channel\\sSubscription\\s(mandeow);msg-param-sub-plan=1000;room-id=128856353;subscriber=1;system-msg=Goop_456789\\sgifted\\sa\\sTier\\s1\\ssub\\sto\\sZackpanjang!;tmi-sent-ts=1678876353907;user-id=93668177;user-type= :tmi.twitch.tv USERNOTICE #mande"
        val ircMessage = IrcMessage.parse(msg)
        val notice = UserNoticeMessage.parseUserNotice(ircMessage) { null }

        assertNotNull(notice)
        assertEquals(expected = "Goop_456789 gifted a Tier 1 sub to Zackpanjang!", actual = notice.message)
        assertEquals(expected = null, actual = notice.childMessage)
        assertEquals(expected = true, actual = notice.isSub)
    }

    @Test
    fun `parse watch streak user notice with key-only tags and no user message`() {
        // real message relayed by recent-messages, empty tags are serialized in key-only form
        val msg =
            "@color;display-name=riese911;tmi-sent-ts=1781130795484;msg-param-value=5;subscriber=0;room-id=129440913;flags;badges;mod=0;id=4d35d5c4-498f-4ca8-beef-e770b5b91a04;msg-param-id=1581787b-197a-471f-aff1-09200b6d4e21;msg-id=viewermilestone;system-msg=riese911\\swatched\\s5\\sconsecutive\\sstreams\\sand\\ssparked\\sa\\swatch\\sstreak!;historical=1;rm-received-ts=1781130795572;badge-info;login=riese911;vip=0;user-type;emotes;user-id=1316509252;msg-param-copoReward=450;msg-param-category=watch-streak :tmi.twitch.tv USERNOTICE #jongie"
        val ircMessage = IrcMessage.parse(msg)
        val notice = UserNoticeMessage.parseUserNotice(ircMessage) { null }

        assertNotNull(notice)
        assertEquals(expected = "", actual = ircMessage.tags["color"])
        assertEquals(expected = "riese911 watched 5 consecutive streams and sparked a watch streak!", actual = notice.message)
        assertEquals(expected = null, actual = notice.childMessage)
        assertEquals(expected = true, actual = notice.isMilestone)
        assertEquals(expected = false, actual = notice.isSub)
    }
}
