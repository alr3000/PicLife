package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

/**
 * Created by alr on 10/20/17.
 *
 * creates a layout with a two-way status dependent icon and text, plus a help message
 * that overlays when the layout is clicked and disappears when it itself is clicked
 */
open class StatusTextView(val context: Context,
                     var okTextId: Int = R.string.ok,
                     var nokTextId: Int = R.string.not_ready,
                     var helpTextId: Int = R.string.blank)  {

    open val TAG = "StatusTextView"

    // resources
    open var okImage = R.drawable.abc_btn_check_to_on_mtrl_015
    open var nokImage = R.drawable.abc_btn_check_to_on_mtrl_000

    var view: ViewGroup? = null
    var image: ImageView? = null
    var text: TextView? = null
    var help: TextView? = null

    protected var isOk: Boolean = false

    fun setOk(ok: Boolean) : StatusTextView {
        isOk = ok
        updateView()
        return this
    }

    fun getOk() : Boolean {
        return isOk
    }

    fun createIn(parent: ViewGroup) : StatusTextView {
       Log.d(TAG, "createIn")
        //todo: -?- replace if present
        view = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater)
                ?.inflate(R.layout.textview_status, null, false) as? ViewGroup
        parent.addView(view)

        view?.setOnClickListener {
            toggleHelp()
        }

        text = (view?.findViewById(R.id.status_text) as? TextView)
        image = (view?.findViewById(R.id.status_image) as? ImageView)
        help = (view?.findViewById(R.id.status_help) as? TextView)

        help?.setText(helpTextId)

        Log.d(TAG, "views: " + listOf(view, text, image, help).joinToString(","))
        updateView()

        return this
    }

    fun addActionButton(button: Button) {
        (view?.findViewById(R.id.status_button_frame) as? ViewGroup)?.addView(button)
    }

    open fun updateView() : StatusTextView {
        Log.d(TAG, "updateView: " + isOk)
        if (isOk) {
            image?.setImageResource(okImage)
            text?.setText(okTextId)
        }
        else {
            image?.setImageResource(nokImage)
            text?.setText(nokTextId)
        }
        return this
    }

    fun toggleHelp() {
        help?.visibility = if (help?.visibility == View.GONE) View.VISIBLE else View.GONE
    }

}