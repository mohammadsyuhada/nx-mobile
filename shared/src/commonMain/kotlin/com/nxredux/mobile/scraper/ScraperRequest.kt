package com.nxredux.mobile.scraper

/** NX Redux `http.c` url_encode: alnum and -_.~ kept, space → '+', every other UTF-8 byte → %XX (uppercase). */
fun ssEncode(s: String): String = buildString {
    for (b in s.encodeToByteArray()) {
        val c = b.toInt() and 0xFF
        val ch = c.toChar()
        when {
            ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '-' || ch == '_' || ch == '.' || ch == '~' -> append(ch)
            ch == ' ' -> append('+')
            else -> { append('%'); append(HEX[c shr 4]); append(HEX[c and 0xF]) }
        }
    }
}
private const val HEX = "0123456789ABCDEF"

/** Developer credentials (build-time) plus the user's optional ScreenScraper login. */
data class ScraperAuth(
    val devId: String, val devPassword: String, val softname: String = "nx-mobile",
    val user: String? = null, val password: String? = null,
) {
    val available: Boolean get() = devId.isNotEmpty() && devPassword.isNotEmpty()
    val signedIn: Boolean get() = !user.isNullOrEmpty() && !password.isNullOrEmpty()
    override fun toString() = "ScraperAuth(available=$available, user=$user)"   // never print secrets
}

/** URL builders in NX Redux's exact parameter order (`scraper_api.c:277-317, 342-346`). */
object ScraperRequest {
    const val API = "https://api.screenscraper.fr/api2"

    private fun dev(a: ScraperAuth) = "devid=${ssEncode(a.devId)}&devpassword=${ssEncode(a.devPassword)}&softname=${ssEncode(a.softname)}"
    private fun user(a: ScraperAuth) = if (a.signedIn) "&ssid=${ssEncode(a.user!!)}&sspassword=${ssEncode(a.password!!)}" else ""

    fun gameInfo(auth: ScraperAuth, fileName: String, systemId: Int, crc32: Long? = null, size: Long? = null): String {
        val base = "$API/jeuInfos.php?${dev(auth)}&romnom=${ssEncode(fileName)}&systemeid=$systemId&output=json${user(auth)}"
        return if (crc32 != null && size != null) base + "&crc=${(crc32 and 0xFFFFFFFFL).toString(16).padStart(8, '0')}&romtaille=$size" else base
    }

    /** `jeuRecherche`: games named like [name], ranked, on [systemId] or (null) every system. */
    fun search(auth: ScraperAuth, name: String, systemId: Int?): String =
        "$API/jeuRecherche.php?${dev(auth)}&recherche=${ssEncode(name)}${systemId?.let { "&systemeid=$it" }.orEmpty()}&output=json${user(auth)}"

    fun userInfo(auth: ScraperAuth): String =
        "$API/ssuserInfos.php?${dev(auth)}&ssid=${ssEncode(auth.user.orEmpty())}&sspassword=${ssEncode(auth.password.orEmpty())}&output=json"
}
