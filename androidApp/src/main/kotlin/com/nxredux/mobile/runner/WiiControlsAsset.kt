package com.nxredux.mobile.runner

import android.content.Context

/** assets/wii/controls.tsv (tools/gametdb), parsed once per process; empty when missing. */
object WiiControlsAsset {
    @Volatile private var cached: WiiControlsTable? = null

    fun get(context: Context): WiiControlsTable = cached ?: synchronized(this) {
        cached ?: runCatching {
            context.assets.open("wii/controls.tsv").use { WiiControlsTable.parse(it.readBytes().decodeToString()) }
        }.getOrDefault(WiiControlsTable.EMPTY).also { cached = it }
    }
}
