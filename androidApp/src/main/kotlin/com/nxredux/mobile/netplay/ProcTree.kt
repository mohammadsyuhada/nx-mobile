package com.nxredux.mobile.netplay

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * netplay: the process tree, read from `/proc` (pure over a proc root, so tests feed it a fake one). rsync in receiving
 * mode forks its receiver, and a kill aimed at the process we started alone orphans that receiver, which keeps writing
 * (nx-redux wizard_sync.c, "PROCESS GROUP"). A JVM `Process` cannot join a process group, so [Rsync.stop] signals the
 * descendants found here instead.
 */
internal object ProcTree {
    /** pid to ppid for every process under [procRoot]; null when [procRoot] is not a directory. */
    fun table(procRoot: File): Map<Int, Int>? {
        val entries = procRoot.listFiles() ?: return null
        val out = HashMap<Int, Int>()
        for (d in entries) {
            val pid = d.name.toIntOrNull() ?: continue
            val stat = runCatching { File(d, "stat").readText() }.getOrNull() ?: continue   // exited mid-scan
            ppidOf(stat)?.let { out[pid] = it }
        }
        return out
    }

    /** The ppid in a `/proc/<pid>/stat` line: field 4, counted after the last ')' since the comm may hold spaces or parens. */
    fun ppidOf(stat: String): Int? {
        val close = stat.lastIndexOf(')')
        if (close < 0) return null
        return stat.substring(close + 1).trim().split(' ').getOrNull(1)?.toIntOrNull()
    }

    /** Every process below [root] in [table], at any depth. */
    fun descendants(table: Map<Int, Int>, root: Int): List<Int> {
        val children = table.entries.groupBy({ it.value }, { it.key })
        val out = LinkedHashSet<Int>()
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty()) {
            for (c in children[queue.removeFirst()].orEmpty()) if (c != root && out.add(c)) queue.addLast(c)
        }
        return out.toList()
    }

    fun descendants(procRoot: File, root: Int): List<Int> = descendants(table(procRoot).orEmpty(), root)

    /** The NUL-separated argv of [pid], or null when it is gone. */
    fun cmdline(procRoot: File, pid: Int): List<String>? =
        runCatching { File(procRoot, "$pid/cmdline").readText() }.getOrNull()?.trimEnd('\u0000')?.split('\u0000')

    /** The child of [parent] whose argv is exactly [args]: how a `Process` is found on Android, which has no `pid()`. */
    fun childWithArgs(procRoot: File, parent: Int, args: List<String>): Int? =
        table(procRoot).orEmpty().entries.firstOrNull { it.value == parent && cmdline(procRoot, it.key) == args }?.key

    /** The pid in a daemon pid file when that process still runs the bundled rsync (a daemon from a killed session). */
    fun staleRsync(procRoot: File, pidFile: File): Int? {
        val pid = runCatching { pidFile.readText().trim().toInt() }.getOrNull() ?: return null
        val argv0 = cmdline(procRoot, pid)?.firstOrNull() ?: return null
        return pid.takeIf { argv0.endsWith(Rsync.FILE_NAME) }
    }

    /** The live table: `/proc` on Android and Linux, `ps` where there is none (a macOS JVM running the tests). */
    fun live(): Map<Int, Int> = table(PROC)?.takeIf { it.isNotEmpty() } ?: ps()

    private fun ps(): Map<Int, Int> = runCatching {
        val p = ProcessBuilder("ps", "-A", "-o", "pid=", "-o", "ppid=").redirectErrorStream(true).start()
        val text = p.inputStream.bufferedReader().readText()
        p.waitFor(2, TimeUnit.SECONDS)
        text.lines().mapNotNull { line ->
            val f = line.trim().split(Regex("\\s+"))
            val pid = f.getOrNull(0)?.toIntOrNull()
            val ppid = f.getOrNull(1)?.toIntOrNull()
            if (pid != null && ppid != null) pid to ppid else null
        }.toMap()
    }.getOrDefault(emptyMap())

    val PROC = File("/proc")
}
