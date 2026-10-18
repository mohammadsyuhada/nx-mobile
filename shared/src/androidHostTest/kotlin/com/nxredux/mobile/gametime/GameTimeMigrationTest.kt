package com.nxredux.mobile.gametime

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nxredux.mobile.gametime.db.GameTimeDb
import java.io.File
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A version 1 database (the shipped schema snapshot) with sessions in it, migrated to the current schema. */
class GameTimeMigrationTest {
    private val file = File.createTempFile("game_time_v1", ".db").also { File("src/commonMain/sqldelight/databases/1.db").copyTo(it, overwrite = true) }
    private val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}", Properties().apply { put("foreign_keys", "true") })

    @AfterTest fun close() { driver.close(); file.delete() }

    @Test fun legacySessionsAreEachTheirOwnPlay() {
        driver.execute(null, "INSERT INTO device(id, name) VALUES ('dev-1', 'Fold')", 0)
        driver.execute(null, "INSERT INTO game(rom_path, name, tag) VALUES ('/Roms/GB/Tetris.gb', 'Tetris', 'GB')", 0)
        driver.execute(null, "INSERT INTO session VALUES ('a', 1, 'dev-1', 'GB', 1000, 61000, 61000, 60000)", 0)
        driver.execute(null, "INSERT INTO session VALUES ('b', 1, 'dev-1', 'GB', 100000, 130000, 130000, 30000)", 0)
        GameTimeDb.Schema.migrate(driver, 1, GameTimeDb.Schema.version)
        val db = GameTimeDb(driver)
        val store = GameTimeStore(db, "dev-1", "Fold", { 200_000L }) { "n" }.also { it.prepare() }
        val t = store.totals().single()
        assertEquals(2L, t.plays); assertEquals(90_000L, t.totalMs)
        assertEquals(listOf("b", "a"), store.sessions(t.gameId).sessions.map { it.uuid })
        assertNull(db.gameTimeQueries.sessionByUuid("a").executeAsOne().play_uuid)
        val indexes = driver.executeQuery(null, "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'game_merge'", { c ->
            app.cash.sqldelight.db.QueryResult.Value(buildList { while (c.next().value) add(c.getString(0)!!) })
        }, 0).value
        assertTrue("game_merge_into" in indexes)
    }
}
