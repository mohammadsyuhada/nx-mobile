package com.nxredux.mobile.storage

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * All files access on Android: [declared] when the manifest lists MANAGE_EXTERNAL_STORAGE and the device runs
 * Android 11+ (a Play build drops the permission: false there); [granted] is `Environment.isExternalStorageManager()`.
 */
class AndroidAllFilesAccess(private val context: Context) : AllFilesAccess {
    override val declared: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions?.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == true
        }.getOrDefault(false)
    }

    override fun granted(): Boolean = isGranted()

    companion object {
        /** Whether All files access is granted, for code without an [AllFilesAccess] at hand. */
        fun isGranted(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
    }

    /** This app's page; some OEM builds lack it, then the list of apps. A non-activity context starts it in a new task. */
    override fun openSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val pages = listOf(
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}")),
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        )
        for (page in pages) {
            if (context !is Activity) page.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(page) }.isSuccess) return
        }
    }
}
