package com.nxredux.mobile.netplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** [ProcTree] on a fake /proc. */
class ProcTreeTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun proc(pid: Int, ppid: Int, comm: String = "rsync", cmdline: List<String> = listOf("x")) {
        val d = File(tmp.root, "$pid").apply { mkdirs() }
        File(d, "stat").writeText("$pid ($comm) S $ppid $pid 0 0 -1 4194560 0 0\n")
        File(d, "cmdline").writeText(cmdline.joinToString("\u0000", postfix = "\u0000"))
    }

    @Test fun stat_ppid_is_read_after_the_last_paren() {
        assertEquals(7, ProcTree.ppidOf("12 (a) b) S 7 12 0"))
        assertEquals(1, ProcTree.ppidOf("12 (x (y)) R 1 12"))
        assertNull(ProcTree.ppidOf("garbage"))
    }

    @Test fun descendants_are_found_at_every_depth_and_nothing_else() {
        proc(100, 1)                 // rsync generator
        proc(101, 100)               // its receiver
        proc(102, 101)               // a grandchild
        proc(200, 1)                 // unrelated
        proc(201, 200)
        File(tmp.root, "self").mkdirs()       // non-numeric entries are skipped
        File(tmp.root, "300").mkdirs()        // a process that exited mid-scan (no stat) is skipped
        assertEquals(listOf(101, 102), ProcTree.descendants(tmp.root, 100).sorted())
        assertEquals(emptyList<Int>(), ProcTree.descendants(tmp.root, 102))
    }

    @Test fun a_ppid_cycle_does_not_loop() {
        assertEquals(listOf(3), ProcTree.descendants(mapOf(2 to 3, 3 to 2), 2))
    }

    @Test fun the_child_with_our_argv_is_found_under_our_pid() {
        val args = listOf("/lib/librsync_exec.so", "--daemon", "--no-detach", "--config=/c/rsyncd.conf")
        proc(10, 1)
        proc(11, 10, cmdline = args)
        proc(12, 10, cmdline = listOf("/lib/librsync_exec.so", "--version"))
        proc(13, 99, cmdline = args)         // same argv, someone else's child
        assertEquals(11, ProcTree.childWithArgs(tmp.root, 10, args))
        assertNull(ProcTree.childWithArgs(tmp.root, 10, listOf("nope")))
    }

    @Test fun a_stale_pid_file_names_a_daemon_only_when_that_pid_runs_our_binary() {
        proc(500, 1, cmdline = listOf("/data/app/lib/arm64/librsync_exec.so", "--daemon"))
        proc(501, 1, cmdline = listOf("/system/bin/sh"))
        val pidFile = tmp.newFile("rsyncd.pid")
        pidFile.writeText("500\n")
        assertEquals(500, ProcTree.staleRsync(tmp.root, pidFile))
        pidFile.writeText("501\n")
        assertNull(ProcTree.staleRsync(tmp.root, pidFile))
        pidFile.writeText("502\n")
        assertNull(ProcTree.staleRsync(tmp.root, pidFile))
        pidFile.writeText("junk")
        assertNull(ProcTree.staleRsync(tmp.root, pidFile))
        assertNull(ProcTree.staleRsync(tmp.root, File(tmp.root, "absent.pid")))
    }
}
