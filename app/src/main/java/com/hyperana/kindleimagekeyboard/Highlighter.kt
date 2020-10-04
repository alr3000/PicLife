package com.hyperana.kindleimagekeyboard

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

class Highlighter(val app: App, val overlay: ViewGroup): ActionListener {
    val TAG = "Highlighter"

    override fun handleAction(action: AACAction, data: Any?): Boolean {
        if (action == AACAction.HIGHLIGHT) {
            (data as? View).also { v ->
                highlightView((v == null), v)
                return true
            }
        }
        return false
    }

    override fun getActionTag(): Int {
        return hashCode()
    }

    fun highlightView(start: Boolean, view: View?) {
        val VIEW_TAG = "actionHighlight"
        val v = overlay.findViewWithTag(VIEW_TAG) as? View
        Log.d(
            TAG,
            "highlightView: $start  (view: ${view?.contentDescription} v=$v)"
        )

        if (start && (v == null) && view != null) {
            val highlight = HighlightView(view, null, app)
            highlight.tag = VIEW_TAG
            overlay.addView(highlight)

            val fadeIn = AlphaAnimation(0.0f, 1.0f)
            val fadeOut = AlphaAnimation(1.0f, 0.0f)
            fadeIn.duration = 500
            fadeOut.duration = 600
            fadeOut.startOffset = 600 + fadeIn.startOffset + 600
            fadeIn.repeatCount = Animation.INFINITE
            fadeOut.repeatCount = Animation.INFINITE

            highlight.startAnimation(fadeIn)
            highlight.startAnimation(fadeOut)
        }
        else if (!start && (v != null)) {
            v.clearAnimation()
            (v.parent as? ViewGroup)?.removeView(v)
        }
        else Log.d(TAG, "highlight view already in chosen config")
    }
}