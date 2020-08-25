package com.hyperana.kindleimagekeyboard

import android.view.View
import androidx.lifecycle.Observer

interface MessageViewManager {
    fun toggleShowMessage(show: Boolean)
    fun hintMessageAction()
    fun getIconListObserver(): Observer<List<IconData>>
}

class RecentsLayout()  {


    fun openMessage() {}



}