package com.nxredux.mobile.runner

import android.content.Context
import com.nxredux.mobile.library.NaomiSets
import com.swordfish.libretrodroid.NxLog

/** assets/system/flycast/sets.tsv (staged by the cores pipeline), parsed once per process; empty when the build lacks it. */
object NaomiSetsAsset {
    @Volatile private var cached: NaomiSets? = null

    fun get(context: Context): NaomiSets = cached ?: synchronized(this) {
        cached ?: runCatching {
            context.assets.open("system/flycast/sets.tsv").use { NaomiSets.parse(it.readBytes().decodeToString()) }
        }.onFailure { NxLog.w("NaomiSetsAsset", "system/flycast/sets.tsv unreadable: NAOMI/Atomiswave titles and BIOS checks are off", it) }
            .getOrDefault(NaomiSets.EMPTY).also { cached = it }
    }
}
