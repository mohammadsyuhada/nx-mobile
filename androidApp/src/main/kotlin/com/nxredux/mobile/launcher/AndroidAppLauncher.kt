package com.nxredux.mobile.launcher

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home through the disabled `.HomeAlias` (manifest) and ROLE_HOME; other apps through their LAUNCHER activity.
 * [requestRole] starts the role dialog for a result.
 */
class AndroidAppLauncher(private val activity: ComponentActivity, private val requestRole: (Intent) -> Unit) : AppLauncher {
    private val pm get() = activity.packageManager
    private val alias get() = ComponentName(activity.packageName, "com.nxredux.mobile.HomeAlias")

    override suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val ouya = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(OUYA_GAME), 0).mapTo(HashSet()) { it.activityInfo.packageName }
        pm.queryIntentActivities(launcherIntent(), 0)
            .filter { it.activityInfo.packageName != activity.packageName }
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                InstalledApp("$pkg/${ri.activityInfo.name}", ri.loadLabel(pm).toString(), game = isGame(ri.activityInfo.applicationInfo) || pkg in ouya)
            }
            .sortedBy { it.label.lowercase() }
    }

    /** The app declares itself a game: its category (API 26+), or the older `android:isGame` flag. */
    private fun isGame(app: ApplicationInfo): Boolean {
        @Suppress("DEPRECATION")
        val flagged = (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        return app.category == ApplicationInfo.CATEGORY_GAME || flagged
    }

    override fun label(id: String): String? {
        val c = component(id) ?: return null
        val info = runCatching { pm.getActivityInfo(c, 0) }.getOrNull() ?: return null
        return info.takeIf { it.enabled && it.exported }?.loadLabel(pm)?.toString()
    }

    /** The activity's icon (its app's when it has none of its own), drawn at [ICON_PX] square: an adaptive icon comes with its mask. */
    override suspend fun icon(id: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val c = component(id) ?: return@withContext null
        runCatching {
            val drawable = runCatching { pm.getActivityIcon(c) }.getOrElse { pm.getApplicationIcon(c.packageName) }
            val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, ICON_PX, ICON_PX)
            drawable.draw(Canvas(bitmap))
            bitmap.asImageBitmap()
        }.getOrNull()
    }

    override fun launch(id: String): Boolean {
        val c = component(id) ?: return false
        return try {
            activity.startActivity(launcherIntent().setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
            true
        } catch (e: ActivityNotFoundException) { false } catch (e: SecurityException) { false }
    }

    override fun isHome(): Boolean =
        if (Build.VERSION.SDK_INT >= 29) activity.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
        else pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == activity.packageName

    override fun setHome(on: Boolean) {
        val state = if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        pm.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP)
        if (on && Build.VERSION.SDK_INT >= 29) {
            val roles = activity.getSystemService(RoleManager::class.java)
            if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) { requestRole(roles.createRequestRoleIntent(RoleManager.ROLE_HOME)); return }
        }
        // Below API 29 (or no role service): Android's own home chooser. Off while still home: let the user pick the old launcher.
        if (on != isHome()) openHomeSettings()
    }

    /**
     * API 26-28 only (29+ answers through the role dialog): [setHome] on left the alias enabled and opened Android's
     * home settings; back without this app picked, the alias goes off again. True when it was turned off.
     */
    fun dropAliasIfNotHome(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) return false
        if (pm.getComponentEnabledSetting(alias) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED || isHome()) return false
        pm.setComponentEnabledSetting(alias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        return true
    }

    private fun openHomeSettings() {
        try { activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        catch (e: ActivityNotFoundException) { runCatching { activity.startActivity(Intent(Settings.ACTION_SETTINGS)) } }   // no home page: the main Settings, else nothing
    }

    private fun launcherIntent() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    private fun component(id: String): ComponentName? =
        id.split('/', limit = 2).takeIf { it.size == 2 && it.all(String::isNotBlank) }?.let { ComponentName(it[0], it[1]) }

    private companion object {
        const val OUYA_GAME = "tv.ouya.intent.category.GAME"   // an OUYA-era game activity: a game even without the category or flag
        const val ICON_PX = 192   // sharp at the menu tiles' 48-60 dp tool icons on a 3x screen; Home's large faint one scales it up
    }
}
