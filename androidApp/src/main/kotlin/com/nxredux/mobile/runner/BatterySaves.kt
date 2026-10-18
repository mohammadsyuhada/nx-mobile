package com.nxredux.mobile.runner

import com.nxredux.mobile.storage.BatterySaveRules
import com.nxredux.mobile.storage.BatteryWrite
import com.nxredux.mobile.storage.NdsDsvSave
import com.nxredux.mobile.storage.NxPaths
import com.nxredux.mobile.storage.Rzip
import com.nxredux.mobile.storage.SaveCopyRules
import com.nxredux.mobile.storage.SaveLookup
import com.swordfish.libretrodroid.NxLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The game's battery save in [dir] (`Saves/<console tag>/`, NxPaths.savesDir), named by [paths]: loaded from the
 * configured name, else the newest file under another name ([SaveLookup]; the next one when a file is damaged or
 * empty), RZIP or raw; written to the configured name in the configured encoding. Other names are never touched (the
 * handheld never deletes them). A configured file that claims RZIP but is malformed ([damaged]) is renamed to
 * `<name>.<yyyyMMdd-HHmmss>.bak` ([backedUpAs]) right before the first write, and saving goes on; when that rename
 * fails it is never overwritten ([writable] = false). As minarch, a file longer than the core's save memory loads its
 * first part (libretrodroid copies at most that size) and is written back at that size.
 * With [dsvStem] set (melonDS DS) and no save under any candidate name, a non-empty `<dsvStem>.dsv` from the handheld
 * is the save ([NdsDsvSave]): loaded without its footer, written back in place with the footer it was read with (none
 * when it had none), never as an `.srm`; the unchanged-skip and atomic write apply as for the others.
 */
class BatterySaves(private val dir: File, private val paths: NxPaths, private val dsvStem: String? = null) {
    private var dsv: NdsDsvSave.Plan? = null
    private val target = File(dir, paths.saveRam.substringAfterLast('/'))
    var writable = true
        private set
    /** The file the save came from, for the log and the toast; null when there was none. */
    var loadedFrom: String? = null
        private set
    /** True from [load] finding the configured file damaged until the first write sets it aside ([backedUpAs]). */
    var damaged = false
        private set
    /** The `.bak` a damaged configured file was renamed to, for the log; null when none. */
    var backedUpAs: String? = null
        private set

    /**
     * The candidates in [SaveLookup.ordered]'s order, until one reads and decodes to non-empty bytes: an unreadable,
     * malformed or empty file never hides a good save under another name.
     */
    fun load(): ByteArray? {
        if (dsvStem != null) {
            val names = paths.saveRamCandidates().map { it.substringAfterLast('/') }
            val plan = NdsDsvSave.plan(dsvStem, names, dir.list()?.toSet() ?: emptySet()) { n -> runCatching { File(dir, n).readBytes() }.getOrNull() }
            if (plan != null) {
                dsv = plan
                loadedFrom = plan.name
                return plan.bytes
            }
        }
        val names = paths.saveRamCandidates().map { it.substringAfterLast('/') }
        for (name in SaveLookup.ordered(names) { n -> File(dir, n).takeIf { it.isFile }?.lastModified() }) {
            val file = File(dir, name)
            val raw = runCatching { file.readBytes() }.getOrNull() ?: continue
            val bytes = Rzip.decode(raw)
            if (bytes == null) {
                // A malformed file under another name is not ours to touch: the next write creates the configured name.
                if (file == target) damaged = true
                continue
            }
            if (bytes.isEmpty()) continue
            loadedFrom = name
            return bytes
        }
        return null
    }

    /**
     * Returns true when written ([BatterySaveRules], on decoded bytes). Empty data (no SRAM, or a core that keeps its own
     * save files) is skipped, as is data equal to the file's content (its mtime stays, so nothing is exported; a
     * compression-only setting change rewrites the file the next time the bytes change). Throws IOException when the
     * write fails.
     */
    fun write(bytes: ByteArray): Boolean = dsv?.let { writeDsv(it, bytes) } ?:
        when (BatterySaveRules.decide(writable, bytes) { if (damaged) null else existingDecoded() }) {
            BatteryWrite.WRITE -> {
                if (damaged && !backUpDamaged()) false
                else { target.writeAtomically(if (paths.saveCompressed) Rzip.encode(bytes) else bytes); true }
            }
            BatteryWrite.UNCHANGED, BatteryWrite.SKIP -> false
        }

    /** The `.dsv` in place: [bytes] plus the footer it was loaded with (sizes matched, [NdsDsvSave.join]); unchanged or empty data is skipped. */
    private fun writeDsv(plan: NdsDsvSave.Plan, bytes: ByteArray): Boolean {
        val file = File(dir, plan.name)
        val decision = BatterySaveRules.decide(writable, bytes) {
            runCatching { file.readBytes() }.getOrNull()?.let { NdsDsvSave.stripDsvFooter(it) ?: it }
        }
        if (decision != BatteryWrite.WRITE) return false
        file.writeAtomically(NdsDsvSave.join(bytes, plan.footer))
        return true
    }

    /**
     * The damaged file is set aside (the save switcher's `.bak` naming) right before the first write replaces it, so a
     * core that never writes (empty SRAM) or a netplay copy (writes skipped) leaves it as it is. False when it could not
     * be: from then on it is never overwritten.
     */
    private fun backUpDamaged(): Boolean {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val bak = File(dir, SaveCopyRules.freeBackupName(target.name, stamp) { File(dir, it).exists() })
        if (target.renameTo(bak)) {
            damaged = false
            backedUpAs = bak.name
            NxLog.w("BatterySaves", "${target.name} is damaged; kept as ${bak.name}")
            return true
        }
        writable = false
        NxLog.e("BatterySaves", "${target.name} is damaged and could not be kept aside; not overwriting it")
        return false
    }

    private fun existingDecoded(): ByteArray? =
        runCatching { target.takeIf { it.isFile }?.readBytes() }.getOrNull()?.let(Rzip::decode)

}
