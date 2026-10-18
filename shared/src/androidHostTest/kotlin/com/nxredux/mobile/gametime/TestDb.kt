package com.nxredux.mobile.gametime

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nxredux.mobile.gametime.db.GameTimeDb
import java.util.Properties

fun testStore(now: () -> Long, deviceId: String = "dev-1", deviceName: String = "Fold"): Pair<GameTimeStore, GameTimeDb> {
    // Foreign keys enforced, so a delete in the wrong order fails here rather than leaving dangling rows.
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
    GameTimeDb.Schema.create(driver)
    val db = GameTimeDb(driver)
    var n = 0
    val store = GameTimeStore(db, deviceId, deviceName, now) { "s-${n++}" }
    store.prepare()
    return store to db
}
