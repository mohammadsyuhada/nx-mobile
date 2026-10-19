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

    /** With All files access: no RetroArch root holds saves or states. */
    const val NOT_FOUND = "No RetroArch saves found in Android/media/com.retroarch/RetroArch"

    /** Without it: the folder the user picked holds no `saves` or `states` folder, or nothing in them. */
    const val NOT_IN_PICKED = "No saves or states folder in the picked folder"
}
