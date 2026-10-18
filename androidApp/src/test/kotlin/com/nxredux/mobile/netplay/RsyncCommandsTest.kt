package com.nxredux.mobile.netplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class RsyncCommandsTest {
    private fun refused(block: () -> Unit) {
        try { block(); fail("accepted") } catch (_: IllegalArgumentException) {}
    }

    @Test fun pull_is_the_handhelds_command_line() {
        assertEquals(
            listOf("/lib/librsync_exec.so", "-t", "--inplace", "--no-perms", "--timeout=8", "--contimeout=4",
                "rsync://10.0.0.2:18731/sync/nxsave.srm", "/data/files/netplay/.staging/"),
            RsyncCommands.pullArgs("/lib/librsync_exec.so", "10.0.0.2", "nxsave.srm", "/data/files/netplay/.staging"),
        )
        assertEquals("/d/", RsyncCommands.pullArgs("/b", "10.0.0.2", "nxsave.sav", "/d/").last())
    }

    @Test fun pull_takes_only_the_two_save_names_and_an_ipv4_host() {
        refused { RsyncCommands.pullArgs("/b", "10.0.0.2", "nxsave.brm", "/d") }
        refused { RsyncCommands.pullArgs("/b", "10.0.0.2", "../nxsave.srm", "/d") }
        refused { RsyncCommands.pullArgs("/b", "host.lan", "nxsave.srm", "/d") }
        refused { RsyncCommands.pullArgs("/b", "10.0.0.2;rm", "nxsave.srm", "/d") }
    }

    @Test fun the_daemon_runs_in_the_foreground_on_our_config() {
        assertEquals(listOf("/b", "--daemon", "--no-detach", "--config=/c/rsyncd.conf"), RsyncCommands.daemonArgs("/b", "/c/rsyncd.conf"))
    }

    @Test fun the_daemon_config_is_exact() {
        val expected = listOf(
            "pid file = /cache/netplay/rsyncd.pid",
            "port = 18731",
            "use chroot = no",
            "hosts allow = 10.0.0.3",
            "reverse lookup = no",
            "",
            "[sync]",
            "  path = /cache/netplay/serve",
            "  read only = true",
            "  list = false",
        ).joinToString("\n", postfix = "\n")
        val conf = RsyncCommands.daemonConf("10.0.0.3", "/cache/netplay/serve", "/cache/netplay/rsyncd.pid")
        assertEquals(expected, conf)
        assertFalse(conf.contains("uid") || conf.contains("gid"))
    }

    @Test fun the_daemon_config_refuses_anything_that_could_add_a_line() {
        refused { RsyncCommands.daemonConf("10.0.0.3\nhosts allow = *", "/s", "/p") }
        refused { RsyncCommands.daemonConf("host.lan", "/s", "/p") }
        refused { RsyncCommands.daemonConf("10.0.0.3", "/s\n[evil]", "/p") }
        refused { RsyncCommands.daemonConf("10.0.0.3", "/s", "/p\r") }
    }
}
