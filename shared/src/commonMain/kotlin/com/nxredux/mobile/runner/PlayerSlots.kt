package com.nxredux.mobile.runner

/** A pad's stable identity across reconnects: on Android its `InputDevice.descriptor` (the device id changes). */
data class DeviceKey(val value: String)

/** One player slot as the Players page shows it. */
sealed interface SlotView {
    data object Empty : SlotView
    data class Playing(val key: DeviceKey) : SlotView
    /** Kept for [key], which disconnected; it gets the slot back when it returns. */
    data class Held(val key: DeviceKey) : SlotView
}

/**
 * Which physical pad is which player, for a core with more than one controller port: slot N is libretro port N. A pad
 * claims the lowest free slot on its first press ([claim]); a disconnected pad's slot is held for it ([disconnected])
 * and given to another pad only when no slot is free. [outOfPlay] pads (a handheld's built-in controls benched on the
 * Players page) never claim. One instance per game session; touch is never a key here.
 */
class PlayerSlots(val players: Int, outOfPlay: Set<DeviceKey> = emptySet()) {
    init { require(players in 2..4) { "players must be 2..4" } }

    /** [port] is the pad's slot; [joined]: it was not live in it before this press (a new or returning pad). */
    data class Claim(val port: Int, val joined: Boolean)

    private val keys = arrayOfNulls<DeviceKey>(players)
    private val live = BooleanArray(players)
    private val benched = outOfPlay.toMutableSet()

    val outOfPlay: Set<DeviceKey> get() = benched.toSet()

    /** The slot [key] is live in, without claiming one; null when it is in none or only held. */
    fun portOf(key: DeviceKey): Int? = keys.indexOf(key).takeIf { it >= 0 && live[it] }

    /** [key] pressed something: its slot, claiming one if it has none. Null: out of play, or every slot is taken. */
    fun claim(key: DeviceKey): Claim? {
        if (key in benched) return null
        val own = keys.indexOf(key)
        if (own >= 0) return Claim(own, joined = !live[own]).also { live[own] = true }
        val slot = keys.indexOfFirst { it == null }.takeIf { it >= 0 }
            ?: live.indexOfFirst { !it }.takeIf { it >= 0 }
            ?: return null
        keys[slot] = key
        live[slot] = true
        return Claim(slot, joined = true)
    }

    /** [key] disconnected: its slot (now held for it), or null when it had none. */
    fun disconnected(key: DeviceKey): Int? = keys.indexOf(key).takeIf { it >= 0 }?.also { live[it] = false }

    /**
     * Puts [key] in [slot] (a connected pad), or empties it for null. A pad already in another slot swaps with whatever
     * [slot] held. Returns the slots whose pad changed, for the host to release their held input.
     */
    fun assign(slot: Int, key: DeviceKey?): Set<Int> {
        if (key == null) {
            if (keys[slot] == null) return emptySet()
            keys[slot] = null; live[slot] = false
            return setOf(slot)
        }
        val from = keys.indexOf(key)
        if (from == slot) return emptySet()
        if (from >= 0) {
            keys[from] = keys[slot]; live[from] = live[slot]
            keys[slot] = key; live[slot] = true
            return setOf(from, slot)
        }
        keys[slot] = key; live[slot] = true
        return setOf(slot)
    }

    /** Benches or un-benches [key]; benching frees its slot, returned (null when it had none). */
    fun setOutOfPlay(key: DeviceKey, out: Boolean): Int? {
        if (!out) { benched.remove(key); return null }
        benched.add(key)
        val slot = keys.indexOf(key).takeIf { it >= 0 } ?: return null
        keys[slot] = null; live[slot] = false
        return slot
    }

    fun slots(): List<SlotView> = keys.indices.map { i ->
        val k = keys[i]
        when {
            k == null -> SlotView.Empty
            live[i] -> SlotView.Playing(k)
            else -> SlotView.Held(k)
        }
    }
}

/**
 * Which connected pads count as "a controller is attached" for the on-screen pad and the layout decisions that follow it
 * (landscape clusters, the portrait band rule, tabletop, DS touch mode). The in-game hints follow every attached pad.
 */
object PadPresence {
    /**
     * [pads] less the Not playing ones in a multiplayer game ([outOfPlay]: its slots' benched pads, [key] each pad's
     * identity), since the game ignores those: benching the only pad brings the touch pad back for Player 1. A one-player
     * game passes null and every pad counts.
     */
    fun <T> attached(pads: List<T>, outOfPlay: Set<DeviceKey>?, key: (T) -> DeviceKey): List<T> =
        if (outOfPlay.isNullOrEmpty()) pads else pads.filter { key(it) !in outOfPlay }
}

/** Pad names for the Players page: a name already shown gets " (2)", " (3)"… so two identical pads tell apart. */
object PadNames {
    fun numbered(names: List<String>): List<String> {
        val seen = HashMap<String, Int>()
        return names.map { n ->
            val count = (seen[n] ?: 0) + 1
            seen[n] = count
            if (count == 1) n else "$n ($count)"
        }
    }
}
