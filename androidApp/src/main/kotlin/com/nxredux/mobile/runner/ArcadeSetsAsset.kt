package com.nxredux.mobile.runner

import android.content.Context
import com.nxredux.mobile.library.ArcadeSets

/** assets/system/fbneo/sets.tsv (staged by the cores pipeline), parsed once per process; empty when the build lacks it. */
object ArcadeSetsAsset {
    @Volatile private var cached: ArcadeSets? = null

    fun get(context: Context): ArcadeSets = cached ?: synchronized(this) {
        cached ?: runCatching {
            context.assets.open("system/fbneo/sets.tsv").use { ArcadeSets.parse(it.readBytes().decodeToString()) }
        }.getOrDefault(ArcadeSets.EMPTY).also { cached = it }
    }
}
