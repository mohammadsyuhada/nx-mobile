package com.nxredux.mobile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.nxredux.mobile.shared.res.Res
import com.nxredux.mobile.shared.res.nx_logo
import com.nxredux.mobile.ui.nx.LocalNxColors
import com.nxredux.mobile.ui.nx.LocalNxFont
import com.nxredux.mobile.ui.nx.nxHighlight
import org.jetbrains.compose.resources.painterResource

/** The welcome's button: its share of the page's width, up to a cap. */
private const val WELCOME_BUTTON_SHARE = 0.6f
private val WELCOME_BUTTON_MAX = 280.dp

/**
 * Onboarding's welcome: the NX mark, the name ([Screen.Onboarding.details] first) and the pitch (second), and one large
 * button (the only row), all centred both ways. No page title, no hint bar: A or a tap on the button is the one way on.
 */
@Composable
internal fun OnboardingWelcomePage(screen: Screen.Onboarding, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalNxColors.current
    val font = LocalNxFont.current
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        val buttonW = min(maxWidth * WELCOME_BUTTON_SHARE, WELCOME_BUTTON_MAX)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(Res.drawable.nx_logo), contentDescription = null, modifier = Modifier.size(width = 80.dp, height = 77.dp), colorFilter = ColorFilter.tint(colors.listText))
            Spacer(Modifier.height(24.dp))
            screen.details.getOrNull(0)?.let {
                Text(it, color = colors.listText, fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, textAlign = TextAlign.Center)
            }
            screen.details.getOrNull(1)?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = colors.hint, fontFamily = font, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(40.dp))
            screen.rows.firstOrNull()?.let { row ->
                val shape = RoundedCornerShape(50)
                Box(
                    Modifier.width(buttonW).height(52.dp).clip(shape).background(nxHighlight(), shape).clickable { onPick(0) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(row.label, color = colors.listTextSelected, fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
            }
        }
    }
}
