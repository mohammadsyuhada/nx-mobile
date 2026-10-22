package com.nxredux.mobile.runner

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.graphics.toArgb
import com.nxredux.mobile.ui.nx.NxDefaults
import kotlin.math.roundToInt

/**
 * The DS stylus cursor in [parent]: a plain, non-clickable dot (touches pass through to what is under it), placed by its
 * centre in px of [parent], sized by [parent]'s display density. Its view is added on the first [show].
 */
internal class PenDot(private val parent: FrameLayout) {
    private var view: View? = null

    /** The dot centred on ([cx], [cy]); full strength while [pressed], else dimmed. */
    fun show(cx: Float, cy: Float, pressed: Boolean) {
        val v = view ?: create()
        val size = (SIZE_DP * parent.resources.displayMetrics.density).roundToInt()
        v.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.START).apply {
            leftMargin = (cx - size / 2f).roundToInt()
            topMargin = (cy - size / 2f).roundToInt()
        }
        v.alpha = if (pressed) 1f else 0.6f
        v.visibility = View.VISIBLE
    }

    fun hide() {
        view?.visibility = View.GONE
    }

    private fun create(): View = View(parent.context).also { v ->
        val density = parent.resources.displayMetrics.density
        v.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(NxDefaults.colors.main.toArgb())   // the UI ink
            setStroke((2 * density).roundToInt(), 0xFF000000.toInt())
        }
        v.isClickable = false
        v.isFocusable = false
        v.visibility = View.GONE
        parent.addView(v, FrameLayout.LayoutParams(0, 0, Gravity.TOP or Gravity.START))
        view = v
    }

    private companion object {
        const val SIZE_DP = 14f   // the stylus cursor's size
    }
}
