package com.nxredux.mobile.storage

/** Where Import saves → From RetroArch looks: RetroArch's own save folders on shared storage. */
object RetroArchFolders {
    /** RetroArch roots relative to shared storage (`/storage/emulated/0`), the Play Store build's first; each one found is read. */
    val ROOTS = listOf(
        "Android/media/com.retroarch/RetroArch",
        "Android/media/com.retroarch.aarch64/RetroArch",
        "Android/media/com.retroarch.ra32/RetroArch",
        "RetroArch",
    )

    /** The folders read under a root. */
    val TOPS = listOf("saves", "states")

    const val NOT_FOUND = "No RetroArch saves found in Android/media/com.retroarch/RetroArch"
}
