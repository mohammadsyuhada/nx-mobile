package com.nxredux.mobile.achievements

import com.swordfish.libretrodroid.LibretroDroid

/** rcheevos' award request builder in the fork; LibretroDroid's static initializer loads liblibretrodroid.so. */
object AndroidRaSigner : RaSigner {
    override fun award(user: String, token: String, achievementId: Int, hash: String, secondsSinceUnlock: Int): Pair<String, String>? =
        LibretroDroid.achievementsBuildAwardRequest(user, token, achievementId, hash, secondsSinceUnlock)
            ?.takeIf { it.size >= 2 }?.let { it[0] to it[1] }
}
