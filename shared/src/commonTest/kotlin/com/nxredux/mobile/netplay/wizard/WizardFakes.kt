package com.nxredux.mobile.netplay.wizard

import kotlinx.coroutines.delay

/** A [SaveSink] that records pulls: [failOn] throws for that name, [pullMs] is each pull's (virtual) duration. */
class FakeSaveSink(override val canPull: Boolean = true, var failOn: String? = null, var pullMs: Long = 0) : SaveSink {
    val pulled = ArrayList<Pair<String, String>>()
    var begun = false
    var committed = false
    var aborted = false
    override fun begin() { begun = true; pulled.clear() }
    override suspend fun pull(hostIp: String, wireName: String) {
        if (pullMs > 0) delay(pullMs)
        if (wireName == failOn) throw IllegalStateException("rsync exited 5")
        pulled += hostIp to wireName
    }
    override fun commit() { committed = true }
    override fun abort() { aborted = true }
}

/** [HostSaves] without a daemon: [start] records the joiner and answers [startOk]. */
class FakeHostSaves(
    override val files: List<String> = emptyList(),
    override val canServe: Boolean = true,
    var startOk: Boolean = true,
) : HostSaves {
    val started = ArrayList<String>()
    var closed = false
    override suspend fun start(joinerIp: String): Boolean { started += joinerIp; return startOk }
    override fun close() { closed = true }
}
