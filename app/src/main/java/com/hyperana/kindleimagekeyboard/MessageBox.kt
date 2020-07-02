package com.hyperana.kindleimagekeyboard

import android.app.Activity
import android.view.Menu
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Observer
import androidx.lifecycle.observe

class MessageBox(val view: ViewGroup) {
    val TAG = "MessageBox"



    // message text observer
    //todo: these should be array of icons, with iconviews, just text, highlight/speak-able
    val messageTextObserver = object : Observer<List<String>> {
        override fun onChanged(t: List<String>?) {
            view.removeAllViews()
            t?.forEachIndexed { index, string ->
                view.addView(TextView(view.context).apply {
                    text = string
                    setOnClickListener {
                        //highlight
                    }
                })
            }
        }
    }


}