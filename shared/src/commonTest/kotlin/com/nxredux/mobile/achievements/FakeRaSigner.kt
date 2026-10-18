package com.nxredux.mobile.achievements

class FakeRaSigner : RaSigner {
    override fun award(user: String, token: String, achievementId: Int, hash: String, secondsSinceUnlock: Int): Pair<String, String>? =
        RaRequests.URL to "r=awardachievement&u=$user&t=$token&a=$achievementId&h=0&m=$hash&o=$secondsSinceUnlock&v=fake"
}
