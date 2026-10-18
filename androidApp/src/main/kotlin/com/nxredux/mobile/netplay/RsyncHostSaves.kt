package com.nxredux.mobile.netplay

import com.nxredux.mobile.netplay.wizard.HostSaves
import java.io.File

/**
 * netplay: the host's saves for one joiner, staged in [stage] as `nxsave.<ext>` and served by the bundled rsync's daemon
 * ([Rsync.startDaemon], config in [work]); no rsync ([rsync] null) means nothing is served.
 *
 * [close] is final and idempotent: a second close (the driver's, after the platform already closed this one to stage
 * the next joiner's saves) waits for the first to finish and never touches the newer staging, and a daemon that comes
 * up after [close] is stopped at once.
 */
internal class RsyncHostSaves(
    override val files: List<String>,
    private val rsync: Rsync?,
    private val stage: File,
    private val work: File,
) : HostSaves {
    private val lock = Any()
    private var daemon: Process? = null
    private var closed = false

    override val canServe: Boolean get() = rsync != null

    override suspend fun start(joinerIp: String): Boolean {
        val r = rsync ?: return false
        if (synchronized(lock) { closed }) return false
        val p = r.startDaemon(joinerIp, stage, work) ?: return false
        val kept = synchronized(lock) { if (closed) false else { daemon = p; true } }
        if (!kept) Rsync.stop(p)
        return kept
    }

    /** Blocking; a concurrent second call waits until the first has stopped the daemon and deleted the staging. */
    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            daemon?.let(Rsync::stop)
            daemon = null
            stage.deleteRecursively()
        }
    }
}
