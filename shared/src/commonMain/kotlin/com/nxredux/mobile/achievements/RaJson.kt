package com.nxredux.mobile.achievements

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object RaJson {
    // coerceInputValues: an explicit null in a non-null field (an achievement's Title, a set's Type) takes the default instead of failing the whole reply.
    val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = false; coerceInputValues = true }
    fun <T> parse(deserializer: DeserializationStrategy<T>, body: String): T? = runCatching { json.decodeFromString(deserializer, body) }.getOrNull()
    /** rc_client's own success test is this literal; the shim caches only such bodies. */
    fun isSuccess(body: String) = body.contains("\"Success\":true")
}

@Serializable data class LoginBody(val Success: Boolean = false, val Error: String? = null, val User: String = "", val Token: String = "", val Score: Int = 0, val SoftcoreScore: Int = 0)
/** login2's point totals, absent when the reply lacks them: [Score] is hardcore, [SoftcoreScore] softcore. */
@Serializable data class LoginTotals(val Score: Int? = null, val SoftcoreScore: Int? = null)
@Serializable data class GameIdBody(val Success: Boolean = false, val GameID: Int = 0)
@Serializable data class SetAchievement(
    val ID: Int, val Title: String = "", val Description: String = "", val Points: Int = 0, val BadgeName: String = "", val Type: String? = null,
    val Flags: Int = 3, val Rarity: Float = 100f, val RarityHardcore: Float = 100f, val BadgeURL: String? = null, val BadgeLockedURL: String? = null,
)
/** One achievement set of an `achievementsets` reply; [Type] is core, bonus, specialty or exclusive, [Title] is null on the core set. */
@Serializable data class SetDefinition(
    val AchievementSetId: Int = 0, val GameId: Int = 0, val Title: String? = null, val Type: String = "core", val ImageIconUrl: String? = null,
    val Achievements: List<SetAchievement> = emptyList(),
)
/** The `achievementsets` reply (rc_api_process_fetch_game_sets_server_response): the game, and its sets nested under [Sets]. */
@Serializable data class SetsBody(
    val Success: Boolean = false, @SerialName("GameId") val gameId: Int = 0, val Title: String = "",
    @SerialName("ConsoleId") val consoleId: Int = 0, val ImageIconUrl: String? = null, val Sets: List<SetDefinition> = emptyList(),
) {
    /** The core-set achievements that count: official (Flags 3), without the Unknown-Emulator warning. */
    val coreAchievements: List<SetAchievement>
        get() = Sets.filter { it.Type == "core" }.flatMap { it.Achievements }.filter { it.Flags == 3 && it.ID != UNKNOWN_EMULATOR_ID }
    val hasAchievements: Boolean get() = coreAchievements.isNotEmpty()
    /** Every set's achievements (core, bonus, specialty, exclusive) without the warning: whose badges are worth prefetching. */
    val allAchievements: List<SetAchievement> get() = Sets.flatMap { it.Achievements }.filter { it.ID != UNKNOWN_EMULATOR_ID }
}
@Serializable data class SessionUnlock(val ID: Int, val When: Long)
/** Unlocks and HardcoreUnlocks are always encoded so a re-encoded (merged or synthetic) body carries both keys. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable data class SessionBody(
    val Success: Boolean = false,
    @EncodeDefault val Unlocks: List<SessionUnlock> = emptyList(),
    @EncodeDefault val HardcoreUnlocks: List<SessionUnlock> = emptyList(),
    val ServerNow: Long? = null,
)
/** The error fields of any RAWeb reply: [Code] is machine-readable (`invalid_credentials`, `not_found`, ...). */
@Serializable data class RaErrorBody(val Success: Boolean = false, val Code: String? = null, val Error: String? = null)
/** The legacy `unlocks` reply (RAWeb GetPlayerGameUnlocksAction): every unlocked id, without times, and no player session. */
@Serializable data class UnlocksBody(val Success: Boolean = false, val UserUnlocks: List<Int> = emptyList())
@Serializable data class AwardBody(val Success: Boolean = false, val Error: String? = null, val Score: Int? = null, val SoftcoreScore: Int? = null, val AchievementID: Int = 0) {
    /** rcheevos treats this server error as a success (rc_api_runtime.c). */
    val alreadyUnlocked: Boolean get() = Error?.startsWith("User already has") == true
}
/** One game of an `allprogress` reply (RAWeb GetUserProgressForConsoleAction; rcheevos reads the same keys): [Unlocked] counts hardcore ones too, absent = 0. */
@Serializable data class GameProgress(val Achievements: Int = 0, val Unlocked: Int = 0, val UnlockedHardcore: Int = 0)
@Serializable data class AllProgressBody(val Success: Boolean = false, val Response: Map<String, GameProgress> = emptyMap())
