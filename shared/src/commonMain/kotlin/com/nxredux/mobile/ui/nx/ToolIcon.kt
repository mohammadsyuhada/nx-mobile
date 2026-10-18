package com.nxredux.mobile.ui.nx

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.nxredux.mobile.launcher.AppIcons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** The session's Android app icons ([AppIcons]); null on a platform without a launcher (every app tile draws the glyph). */
val LocalAppIcons = staticCompositionLocalOf<AppIcons?> { null }

/** App [id]'s icon: the cached one at once, else loaded in the background; null with no [id], no icons, or none loaded. */
@Composable
internal fun rememberAppIcon(id: String?): ImageBitmap? {
    val icons = LocalAppIcons.current
    if (id == null || icons == null) return null
    val icon by produceState(icons.cached(id), icons, id) { value = icons.load(id) }
    return icon
}

/**
 * A tool's icon: an Android app ([appId]) with its own icon draws it in full colour; any other tool (or an app whose
 * icon is missing or still loading) draws its white glyph [icon] tinted [tint].
 */
@Composable
internal fun ToolIcon(icon: DrawableResource, appId: String?, tint: Color, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val app = rememberAppIcon(appId)
    if (app != null) Image(app, contentDescription = null, modifier = modifier, contentScale = contentScale)
    else Image(painterResource(icon), contentDescription = null, modifier = modifier, contentScale = contentScale, colorFilter = ColorFilter.tint(tint))
}
