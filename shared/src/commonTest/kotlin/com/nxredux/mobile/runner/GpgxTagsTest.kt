package com.nxredux.mobile.runner

import com.nxredux.mobile.achievements.RaConsoles
import com.nxredux.mobile.cheats.CheatFolders
import com.nxredux.mobile.netplay.NetplayCores
import com.nxredux.mobile.netplay.NetplayPads
import com.nxredux.mobile.netplay.wizard.Caps
import com.nxredux.mobile.netplay.wizard.Reject
import com.nxredux.mobile.netplay.RejectReason
import com.nxredux.mobile.options.OverlayResolver
import com.nxredux.mobile.scraper.ScraperSystems
import com.nxredux.mobile.storage.NxPaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Genesis Plus GX on Master System, Game Gear, Sega CD and SG-1000: GPGX_<X> here, the handheld's GPGX at its boundaries. */
class GpgxTagsTest {
    private val internal = listOf("GPGX_SMS", "GPGX_GG", "GPGX_SEGACD", "GPGX_SG1000")

    @Test fun each_sega_console_has_genesis_plus_gx_second() {
        assertEquals(listOf("SMS", "GPGX_SMS"), CoreRegistry.consoleOf("SMS")!!.tags)
        assertEquals(listOf("GG", "GPGX_GG"), CoreRegistry.consoleOf("GG")!!.tags)
        assertEquals(listOf("SEGACD", "GPGX_SEGACD"), CoreRegistry.consoleOf("SEGACD")!!.tags)
        assertEquals(listOf("SG1000", "GPGX_SG1000"), CoreRegistry.consoleOf("SG1000")!!.tags)
        for (t in internal) assertEquals("genesis_plus_gx", CoreRegistry.forTag(t)!!.id, t)
        assertEquals("md", CoreRegistry.consoleOf("GPGX")!!.id)   // GPGX itself stays Genesis's
        assertEquals("SMS", CoreRegistry.consoleTag("GPGX_SMS"))
    }

    @Test fun handheld_and_base_tags() {
        for (t in internal) assertEquals("GPGX", CoreRegistry.handheldTag(t), t)
        assertEquals("GPGX", CoreRegistry.handheldTag("gpgx"))
        assertEquals("MGBA", CoreRegistry.handheldTag("MGBA"))
        assertEquals("SMS", CoreRegistry.baseTag("GPGX_SMS"))
        assertEquals("SEGACD", CoreRegistry.baseTag("gpgx_segacd"))
        assertEquals("GPGX", CoreRegistry.baseTag("GPGX"))
        val sms = CoreRegistry.consoleOf("SMS")!!
        assertEquals("GPGX_SMS", CoreRegistry.phoneTag(sms, "GPGX"))
        assertEquals("SMS", CoreRegistry.phoneTag(sms, "sms"))
        assertEquals("GPGX", CoreRegistry.phoneTag(CoreRegistry.consoleOf("MD")!!, "GPGX"))
        assertNull(CoreRegistry.phoneTag(sms, "MGBA"))
    }

    @Test fun a_gpgx_sms_game_keeps_its_states_where_the_handhelds_gpgx_pak_does() {
        val p = NxPaths("GPGX_SMS", CoreRegistry.forTag("GPGX_SMS")!!.id, "Alex Kidd.sms")
        assertEquals(".userdata/shared/GPGX-genesis_plus_gx", p.statesDir)
        assertEquals(".userdata/shared/.minui/GPGX", p.minuiDir)
        assertEquals("Saves/SMS", p.savesDir)
        assertEquals(".userdata/android/GPGX_SMS-genesis_plus_gx", p.configDir)   // this phone's own: per console
    }

    @Test fun labels_show_the_handhelds_tag() {
        assertEquals("Genesis Plus GX (GPGX)", CoreRegistry.emulatorLabel("GPGX_SMS"))
        assertEquals("Genesis Plus GX (GPGX)", CoreRegistry.emulatorLabel("GPGX"))
        assertEquals("PicoDrive (SMS)", CoreRegistry.emulatorLabel("SMS"))
    }

    @Test fun console_specific_tables_read_the_consoles_own_row() {
        assertEquals(CoreRegistry.requiredBios("SEGACD"), CoreRegistry.requiredBios("GPGX_SEGACD"))   // a disc needs the Sega CD BIOS
        assertTrue(CoreRegistry.requiredBios("GPGX_SMS").isEmpty())
        assertEquals(1, CoreRegistry.playersFor("GPGX_GG", CoreRegistry.forTag("GPGX_GG")))
        assertEquals(2, CoreRegistry.playersFor("GPGX_SMS", CoreRegistry.forTag("GPGX_SMS")))
        assertTrue(PadMap.isSegaPadTag("GPGX_SEGACD"))   // Controller Type, as Sega CD
        assertTrue(!PadMap.isSegaPadTag("GPGX_SMS"))
        assertEquals("1", PadMap.forTag("GPGX_SMS").label(com.nxredux.mobile.ui.nx.NxButton.B))   // the Master System pad
        assertEquals(listOf("SMS"), CheatFolders.tagsFor("GPGX_SMS", "Alex Kidd.sms"))
        assertEquals("Cheats/GG", CheatFolders.primary("GPGX_GG", "Shinobi.gg"))
        assertEquals("Overlays/GG", OverlayResolver.folder("GPGX_GG"))
        assertEquals(RaConsoles.MASTER_SYSTEM, RaConsoles.consoleId("GPGX_SMS", "Alex Kidd.sms"))
        assertEquals(RaConsoles.SEGA_CD, RaConsoles.consoleId("GPGX_SEGACD", "Sonic CD.chd"))
        assertEquals(ScraperSystems.systemIdFor("SG1000", "x.sg"), ScraperSystems.systemIdFor("GPGX_SG1000", "x.sg"))
    }

    @Test fun netplay_runs_the_gpgx_paks_cores_and_sends_its_tag() {
        for (t in internal) {
            assertEquals(listOf("genesis_plus_gx", "picodrive"), NetplayCores.offered(t), t)   // the GPGX pak's default first
            assertTrue(NetplayCores.supported(t), t)
        }
        assertNotNull(NetplayPads.forSession("GPGX_SMS", "genesis_plus_gx"))
        assertEquals(NetplayPads.forSession("SMS", "picodrive"), NetplayPads.forSession("GPGX_SMS", "picodrive"))
        assertEquals("core=genesis_plus_gx,tag=GPGX", Caps.of("genesis_plus_gx", "GPGX_SMS"))
        assertEquals("REJECT core-genesis_plus_gx.GPGX", Reject(RejectReason.CORE, "genesis_plus_gx", "GPGX_GG").format())
    }

    @Test fun bios_folders_are_the_handhelds() {
        for (t in internal) assertTrue(BiosFolders.forTag(t).none { it.startsWith("GPGX_") }, t)
        assertTrue("MD" in BiosFolders.forTag("GPGX_SEGACD"))   // the Brick keeps the Sega CD BIOS in Bios/MD
    }
}
