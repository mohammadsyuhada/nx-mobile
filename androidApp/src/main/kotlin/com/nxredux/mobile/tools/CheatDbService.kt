package com.nxredux.mobile.tools

import android.content.Context
import com.nxredux.mobile.cheats.db.CheatDbController
import com.nxredux.mobile.runner.CoreRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** One controller per process, on a scope no activity cancels: a download survives rotation and a game launch. */
object CheatDbService {
    @Volatile private var instance: CheatDbController? = null

    fun controller(context: Context): CheatDbController = instance ?: synchronized(this) {
        instance ?: CheatDbController(
            AndroidCheatDb(context.applicationContext), CoreRegistry.cheatTags,
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), System::currentTimeMillis,
        ).also { instance = it }
    }
}
