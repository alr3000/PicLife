package com.hyperana.kindleimagekeyboard

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.AdapterView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView

class MessageViewController (
    val app: App,
    val lifecycleOwner: LifecycleOwner,
    val model: IconListModel? = null,
    val inputter: WordInputter?,
    val overlay: ViewGroup,
    val backspaceView: View? = null,
    val forwardDeleteView: View? = null,
    val inputActionView: View? = null,
    val messageViewContainer: View? = null
) : IconListener
{
    val TAG = "InputViewController"

    val messageObserver = object: Observer<List<IconData>> {
        override fun onChanged(t: List<IconData>?) {
            try {
                val isNotEmpty = t?.isNotEmpty() ?: false
                // highlight done button if text not empty
                if (app.get("doActionHighlight")?.toString()?.toBoolean() ?: true) {
                    highlightActionButton(isNotEmpty)
                }

                //updateMessageView(t)

            } catch (e: Exception) {
                Log.w(TAG, "failed done button highlight", e)
            }
        }
    }

    val messageCursorObserver = object: Observer<Int> {
        override fun onChanged(t: Int?) {
            Log.d(TAG, "cursor at: $t")
        }
    }



    init {
        backspaceView?.setOnClickListener {
            inputter?.backwardDelete()
        }
        forwardDeleteView?.setOnClickListener {
            inputter?.forwardDelete()
        }
        inputActionView?.setOnClickListener {
            inputter?.action()
        }
        model?.icons?.observe(lifecycleOwner, messageObserver)
        model?.index?.observe(lifecycleOwner, messageCursorObserver)


    }

    // icon interface:
    override fun onIconEvent(icon: IconData?, action: AACAction?, view: View?) {
        when (action) {
            AACAction.ICON_EXECUTE -> execute(icon, view)
            else -> {}
        }
    }

    fun execute(icon: IconData?, v: View?) {
        Log.d(TAG, "execute: " + icon?.text)
        try {
            val typeLinks = app.get("doTypeLinks")?.toString()?.toBoolean() ?: false
            if ((icon?.linkToPageId != null) && (!typeLinks)) {
                return
            }
            if (icon?.text == null) {
                return
            }
            inputter?.input(icon.text!!)
        } catch(e: Exception) {
            Log.e(TAG, "problem with icon input", e)
        }
    }

    fun highlightActionButton(start: Boolean) {
        val VIEW_TAG = "actionHighlight"
        val v = overlay?.findViewWithTag(VIEW_TAG) as? View
        Log.d(TAG, "highlightActionButton: " + start + " v=" + v.toString())

        if (start && (v == null) && inputActionView != null) {
            val highlight = HighlightView(inputActionView, null, app)
            highlight.tag = VIEW_TAG
            overlay?.addView(highlight)

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
            assert(
                (overlay?.findViewWithTag<View>(VIEW_TAG) == null),
                {"highlight view remaining. " + v.toString()}
            )
        }

    }


}