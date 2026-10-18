package com.nxredux.mobile.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RaJsonTest {
    @Test fun login_body() {
        val b = RaJson.parse(LoginBody.serializer(), """{"Success":true,"User":"me","Token":"tok","Score":10,"SoftcoreScore":20,"Messages":1,"AvatarUrl":"x"}""")!!
        assertEquals("me", b.User); assertEquals("tok", b.Token); assertEquals(20, b.SoftcoreScore); assertTrue(b.Success)
        assertEquals("bad", RaJson.parse(LoginBody.serializer(), """{"Success":false,"Error":"bad"}""")!!.Error)
    }
    @Test fun sets_body_reads_only_what_the_browser_needs() {
        val b = RaJson.parse(SetsBody.serializer(), setsJson(42, consoleId = 5, achievements = """{"ID":1,"Title":"A","Description":"D","Flags":3,"Points":5,"MemAddr":"0x","BadgeName":"00001","Created":1,"Modified":2,"Type":"missable","Rarity":12.5,"RarityHardcore":3.0},{"ID":2,"Flags":5}"""))!!
        assertEquals(42, b.gameId); assertEquals(5, b.consoleId); assertEquals(1, b.coreAchievements.size)   // Flags 5 (unofficial) is not counted
        val a = b.coreAchievements[0]
        assertEquals("missable", a.Type); assertEquals(12.5f, a.Rarity); assertEquals(3, a.Flags)
    }
    @Test fun sets_body_parses_the_real_achievementsets_shape() {
        // A trimmed achievementsets reply as cached on a phone (Alleyway): the warning and one real achievement.
        val b = RaJson.parse(SetsBody.serializer(), """{"Success":true,"GameId":682,"Title":"Alleyway","ImageIconUrl":"https://media.retroachievements.org/Images/036006.png","RichPresenceGameId":682,"ConsoleId":4,"Sets":[{"Title":null,"Type":"core","AchievementSetId":564,"GameId":682,"ImageIconUrl":"https://media.retroachievements.org/Images/036006.png","Achievements":[{"ID":101000001,"Title":"Warning: Unknown Emulator","Description":"Hardcore unlocks cannot be earned using this emulator.","Points":0,"Author":"","Modified":1790381489,"Created":1790381489,"BadgeName":"00000","Flags":3,"Type":null,"Rarity":0,"RarityHardcore":0,"BadgeURL":"https://media.retroachievements.org/Badge/00000.png","BadgeLockedURL":"https://media.retroachievements.org/Badge/00000_lock.png"},{"ID":27252,"Title":"Seven Lucky Paddles","Description":"Have 7 lives at once","Points":5,"Author":"Kasumariii","Modified":1593023570,"Created":1438974253,"BadgeName":"27720","Flags":3,"Type":null,"Rarity":28.25,"RarityHardcore":12.55,"BadgeURL":"https://media.retroachievements.org/Badge/27720.png","BadgeLockedURL":"https://media.retroachievements.org/Badge/27720_lock.png"}],"Leaderboards":[]}]}""")!!
        assertEquals(682, b.gameId); assertEquals(4, b.consoleId); assertEquals("Alleyway", b.Title)
        assertNull(b.Sets.single().Title); assertEquals("core", b.Sets.single().Type)
        assertEquals(1, b.coreAchievements.size); assertTrue(b.hasAchievements)   // the Unknown-Emulator warning is excluded
        val a = b.coreAchievements.single()
        assertEquals(27252, a.ID); assertEquals("27720", a.BadgeName); assertEquals("https://media.retroachievements.org/Badge/27720_lock.png", a.BadgeLockedURL)
    }
    @Test fun the_full_phone_sample_parses() {
        val b = RaJson.parse(SetsBody.serializer(), alleywaySetsJson)!!   // "Warning" and the leaderboards are ignored
        assertEquals(682, b.gameId); assertEquals(4, b.consoleId)
        assertEquals(16, b.coreAchievements.size)   // 18 minus the warning minus the one with Flags 5
        assertEquals(17, b.allAchievements.size)
    }
    @Test fun explicit_nulls_fall_back_to_the_defaults() {
        val b = RaJson.parse(SetsBody.serializer(), """{"Success":true,"GameId":1,"ConsoleId":2,"Sets":[{"Type":null,"Achievements":[{"ID":5,"Title":null,"Description":null,"Flags":3}]}]}""")!!
        assertEquals(listOf(5), b.coreAchievements.map { it.ID }); assertEquals("", b.coreAchievements[0].Title)
    }
    @Test fun only_core_sets_count() {
        val body = """{"Success":true,"GameId":1,"ConsoleId":2,"Sets":[{"Type":"core","Achievements":[{"ID":1}]},{"Type":"bonus","Achievements":[{"ID":2}]}]}"""
        assertEquals(listOf(1), RaJson.parse(SetsBody.serializer(), body)!!.coreAchievements.map { it.ID })
        assertEquals(listOf(1, 2), RaJson.parse(SetsBody.serializer(), body)!!.allAchievements.map { it.ID })
    }
    @Test fun session_award_and_allprogress() {
        val s = RaJson.parse(SessionBody.serializer(), """{"Success":true,"Unlocks":[{"ID":1,"When":100}],"HardcoreUnlocks":[],"ServerNow":5}""")!!
        assertEquals(listOf(SessionUnlock(1, 100L)), s.Unlocks)
        val a = RaJson.parse(AwardBody.serializer(), """{"Success":false,"Error":"User already has this achievement awarded.","Score":1,"SoftcoreScore":2,"AchievementID":9}""")!!
        assertTrue(a.alreadyUnlocked)
        val p = RaJson.parse(AllProgressBody.serializer(), """{"Success":true,"Response":{"42":{"Achievements":10,"Unlocked":3}}}""")!!
        assertEquals(3, p.Response.getValue("42").Unlocked)
    }
    @Test fun garbage_is_null_and_success_scan_is_literal() {
        assertNull(RaJson.parse(LoginBody.serializer(), "not json"))
        assertTrue(RaJson.isSuccess("""{"Success":true,"X":1}""")); assertTrue(!RaJson.isSuccess("""{"Success":false}"""))
    }
}
