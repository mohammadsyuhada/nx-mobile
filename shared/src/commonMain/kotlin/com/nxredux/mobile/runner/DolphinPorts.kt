package com.nxredux.mobile.runner

/**
 * Dolphin's ports for a session of [players]: port 0 and every [claimed] port (a player slot a controller holds) get
 * [device]; the rest get `RETRO_DEVICE_NONE`. Dolphin connects a Wiimote per port that has a device, and Galaxy-style
 * games show a co-star cursor for every connected remote, so players 2-4 stay unplugged until a controller takes them.
 */
fun dolphinPorts(device: Int, claimed: Set<Int>, players: Int): Map<Int, Int> =
    (0 until maxOf(players, 1)).associateWith { if (it == 0 || it in claimed) device else CoreDescriptor.NONE }
