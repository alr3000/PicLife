package com.hyperana.kindleimagekeyboard

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

class InputViewController (
    val inputter: WordInputter?,
    val backspaceView: View? = null,
    val forwardDeleteView: View? = null,
    val inputActionView: View? = null
)
{
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
    }


}