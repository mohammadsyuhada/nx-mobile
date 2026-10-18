package com.nxredux.mobile.gametime

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.nxredux.mobile.gametime.db.GameTimeDb
import com.nxredux.mobile.runner.NxStorage
import com.nxredux.mobile.settings.AndroidAppSettings
import com.nxredux.mobile.storage.NxPaths
import com.swordfish.libretrodroid.NxLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.UUID

/**
 * Process singleton: the game-time database (app-private `.userdata/shared/game_time.db`), its recorder for
 * GameActivity and its data for the Game Time screen. A database that can't be opened leaves recording off and the
 * screen showing an error; play is never affected. A failed open is retried on a later call (at most every
 * [RETRY_MS]), so a full disk or a locked file does not turn game time off until the process dies. Both accessors
 * may block (they open the database): call them off the main thread.
 */
object GameTimeService {
    private const val TAG = "GameTime"
    @OptIn(ExperimentalCoroutinesApi::class)
    private val serial = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private const val RETRY_MS = 10_000L
    @Volatile private var opener: RetryingOpen<GameTimeStore>? = null
    @Volatile private var recorder: GameTimeRecorder? = null

    private fun store(context: Context): GameTimeStore? {
        val app = context.applicationContext
        val o = opener ?: synchronized(this) {
            opener ?: RetryingOpen(RETRY_MS, SystemClock::elapsedRealtime, { NxLog.e(TAG, "game time database unavailable", it) }) { open(app) }
                .also { opener = it }
        }
        return o.get()
    }

    private fun open(app: Context): GameTimeStore {
        val file = File(NxStorage.root(app), NxPaths.GAME_TIME_DB).apply { parentFile?.mkdirs() }
        val driver = AndroidSqliteDriver(GameTimeDb.Schema, app, file.absolutePath)
        return try {
            val name = Settings.Global.getString(app.contentResolver, Settings.Global.DEVICE_NAME) ?: Build.MODEL
            GameTimeStore(GameTimeDb(driver), AndroidAppSettings(app).gameTimeDeviceId, name, System::currentTimeMillis) {
                UUID.randomUUID().toString()
            }.also { it.prepare() }
        } catch (e: Throwable) {
            runCatching { driver.close() }   // the next try opens its own
            throw e
        }
    }

    /** Kept only once it has the database: a recorder made without one records nothing, so the next game tries again. */
    fun recorder(context: Context): GameTimeRecorder =
        recorder ?: synchronized(this) {
            recorder ?: store(context).let { s ->
                GameTimeRecorder(s, serial) { NxLog.e(TAG, "game time write failed", it) }.also { if (s != null) recorder = it }
            }
        }

    fun data(context: Context): GameTimeData? = store(context)?.let { StoreGameTimeData(it, Dispatchers.IO) }
}

/**
 * [open]'s value once it succeeds, kept for good; a failure is not kept: [get] answers null and tries again on a call
 * at least [retryMs] after it ([now] is a monotonic clock). Thread-safe; one open at a time.
 */
internal class RetryingOpen<T : Any>(
    private val retryMs: Long,
    private val now: () -> Long,
    private val onFailure: (Throwable) -> Unit,
    private val open: () -> T,
) {
    @Volatile private var value: T? = null
    private var failedAt: Long? = null

    fun get(): T? = value ?: synchronized(this) {
        value ?: run {
            val t = now()
            if (failedAt?.let { t - it < retryMs } == true) return@run null
            runCatching(open).onSuccess { value = it; failedAt = null }.onFailure { failedAt = t; onFailure(it) }.getOrNull()
        }
    }
}
