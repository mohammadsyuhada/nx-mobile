package com.swordfish.libretrodroid

/** One rc_client event; [type] is a `LibretroDroid.RA_EVENT_*` value. Fields not meaningful for the type are empty/zero. */
data class AchievementEvent(
    val type: Int, val id: Int, val title: String, val description: String, val badge: String,
    val points: Int, val progress: String, val percent: Float, val extra: String,
)

/** An HTTP request rc_client wants sent; answer with `LibretroDroid.achievementsServerResponse(requestId, …)`. */
data class AchievementServerCall(val requestId: Int, val url: String, val postData: String, val contentType: String)
