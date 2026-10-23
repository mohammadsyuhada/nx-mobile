package com.nxredux.mobile.launcher

import android.app.ActivityOptions
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
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
            val drawable = launcherIcon(c)
            val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, ICON_PX, ICON_PX)
            drawable.draw(Canvas(bitmap))
            bitmap.asImageBitmap()
        }.getOrNull()
    }

    /**
     * An adaptive icon's themed-icon layer (API 33+), else its foreground without the background, as [ICON_PX] of
     * white on clear framed by [IconSilhouette]; null for a legacy icon, which has no layer apart from its plate.
     */
    override suspend fun silhouette(id: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val c = component(id) ?: return@withContext null
        runCatching {
            val icon = launcherIcon(c) as? AdaptiveIconDrawable ?: return@runCatching null
            val layer = (if (Build.VERSION.SDK_INT >= 33) icon.monochrome else null) ?: icon.foreground ?: return@runCatching null
            // The layer drawn as the icon draws it: half as large again as the visible square, which shows its middle.
            val shape = Bitmap.createBitmap(SHAPE_PX, SHAPE_PX, Bitmap.Config.ARGB_8888)
            layer.setBounds(-SHAPE_PX / 4, -SHAPE_PX / 4, SHAPE_PX + SHAPE_PX / 4, SHAPE_PX + SHAPE_PX / 4)
            layer.draw(Canvas(shape))
            // The shape alone, white: the tile tints it like a glyph.
            val pixels = IntArray(SHAPE_PX * SHAPE_PX).also { shape.getPixels(it, 0, SHAPE_PX, 0, 0, SHAPE_PX, SHAPE_PX) }
            val alpha = IntArray(pixels.size) { IconSilhouette.shapeAlpha(pixels[it] ushr 24) }
            shape.setPixels(IntArray(pixels.size) { (alpha[it] shl 24) or 0xFFFFFF }, 0, SHAPE_PX, 0, 0, SHAPE_PX, SHAPE_PX)
            val frame = IconSilhouette.frame(alpha, SHAPE_PX) ?: return@runCatching null
            val out = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
            val k = ICON_PX / frame.side
            val fit = Matrix().apply { setTranslate(-frame.x, -frame.y); postScale(k, k) }
            Canvas(out).drawBitmap(shape, fit, Paint(Paint.FILTER_BITMAP_FLAG))
            shape.recycle()
            out.asImageBitmap()
        }.getOrNull()
    }

    private fun launcherIcon(c: ComponentName) = runCatching { pm.getActivityIcon(c) }.getOrElse { pm.getApplicationIcon(c.packageName) }

    /**
     * Dual mode's top screen, where an app opens ([topScreenDisplay], set by MainActivity): another display's id, this
     * activity's own, or null outside dual mode (launch as usual).
     */
    var topDisplay: () -> Int? = { null }

    /**
     * In dual mode on the top screen ([launchRoute]): on this activity's display with the launch display named (an app
     * still on the other display moves here); on another display through a [TopScreenHost], falling back to the normal
     * launch when that display takes no activity.
     */
    override fun launch(id: String): Boolean {
        val c = component(id) ?: return false
        val app = launcherIntent().setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val top = topDisplay()
        val own = activity.currentDisplayId()
        val route = launchRoute(top, own)
        if (route == LaunchRoute.HOST && pm.resolveActivity(app, 0) != null && TopScreenHost.start(activity, app, top!!, own)) return true
        return try {
            activity.startActivity(app, if (route == LaunchRoute.USUAL) null else ActivityOptions.makeBasic().setLaunchDisplayId(own).toBundle())
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
        const val SHAPE_PX = 288  // the visible square a silhouette is cut from: its logo, about two thirds of it, still fills ICON_PX
    }
}
