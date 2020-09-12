package com.hyperana.kindleimagekeyboard

import android.view.View

//todo: combine these as action listener with data: Any? and findviewbytag in response
data class IconEvent(val icon: IconData?, val action: AACAction?, val view: View?) {
    override fun toString(): String {
        return "IconEvent[$action, ${icon?.text}"
    }
}

interface IconListener {
    fun onIconEvent(icon: IconData?, action: AACAction? = null, view: View? = null)
}
