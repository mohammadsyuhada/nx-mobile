package com.nxredux.mobile.netplay

/**
 * The core options a link session gives its core before load, as the handheld sets them: nx-redux
 * `gblink.c` (gambatte's link mode, port and the host's address as 12 digits) and `gbalink_mode.c` (gpSP's
 * `gpsp_serial` for the transport). Lockstep cores get none.
 */
object NetplayLinkOptions {
    const val GPSP_SERIAL = "gpsp_serial"
    const val GPSP_DEFAULT = "auto"
    const val GB_MODE = "gambatte_gb_link_mode"
    const val GB_PORT_KEY = "gambatte_gb_link_network_port"
    const val GB_SERVER = "Network Server"
    const val GB_CLIENT = "Network Client"
    /** gambatte re-reads the mode on a variable update: this one stops its network serial (Leave in a GB link session). */
    const val GB_OFF = "Not Connected"
    const val GB_PORT = 56400

    fun gbIpKey(digit: Int) = "gambatte_gb_link_network_server_ip_$digit"

    /** gbalink_serial_option: over USB `auto` is `auto_cable` (real cable unless the game has a wireless adapter); elsewhere `lockstep` is `auto`. */
    fun gpspSerial(value: String, overUsb: Boolean): String = when {
        overUsb && value == "auto" -> "auto_cable"
        !overUsb && value == "lockstep" -> "auto"
        else -> value
    }

    /** "192.168.1.7" -> "192168001007" (each octet zero-padded to 3); null when [ip] is not dotted IPv4. */
    fun ipDigits(ip: String): String? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        val octets = parts.map { p -> p.takeIf { it.isNotEmpty() && it.length <= 3 && it.all(Char::isDigit) }?.toInt()?.takeIf { it in 0..255 } ?: return null }
        return octets.joinToString("") { it.toString().padStart(3, '0') }
    }

    /** The host's READY mode: gpSP's raw `gpsp_serial` ([own], or the core default); "" for mGBA, which has none. */
    fun hostMode(coreId: String, own: String?): String = if (coreId == "gpsp") own ?: GPSP_DEFAULT else ""

    /** The joiner's mode: the host's from READY ([host]); its own when the host sent none; "" for mGBA. */
    fun joinerMode(coreId: String, own: String?, host: String): String =
        if (coreId != "gpsp") "" else host.ifEmpty { own ?: GPSP_DEFAULT }

    /** The options [session]'s core is launched with; [linkMode]: gpSP's agreed raw mode (null: the default). */
    fun forSession(session: NetplaySession, linkMode: String?): Map<String, String> = when (NetplayCores.link(session.coreId)) {
        NetplayLinkKind.GBLINK -> buildMap {
            put(GB_PORT_KEY, GB_PORT.toString())
            if (session.role == NetplayRole.HOST) {
                put(GB_MODE, GB_SERVER)
            } else {
                put(GB_MODE, GB_CLIENT)
                (ipDigits(session.peerIp) ?: "0".repeat(12)).forEachIndexed { i, d -> put(gbIpKey(i + 1), d.toString()) }
            }
        }
        NetplayLinkKind.NETPACKET ->
            if (session.coreId == "gpsp") mapOf(GPSP_SERIAL to gpspSerial(linkMode?.ifEmpty { null } ?: GPSP_DEFAULT, session.overUsb))
            else emptyMap()
        null -> emptyMap()
    }
}
