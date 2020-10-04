package com.hyperana.kindleimagekeyboard

import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.*

// handles aac icon actions, such as deep links (not swipe paging) and overlay graphics
// handles view updates outside of the pager (title, back)
class AACManager (
    val app: App,
    val overlay: ViewGroup?,
    val aacViewModel: AACViewModel,
    val gotoHomeView: View?,
    val titleView: TextView?,
    val actionManager: ActionManager
)
    :   IconListener, ActionListener
{

    val TAG = "AACManager"


    init {

        titleView?.also { title ->
            aacViewModel.liveCurrentPage.observe(title.context as LifecycleOwner) {
                title.text = it.name
            }
        }

        gotoHomeView?.apply {
            visibility =
                if (app.get("doHomeButton")?.toString()?.toBoolean() ?: true) View.VISIBLE
                else View.INVISIBLE

            setOnClickListener { v -> doClickHome(v) }
        }

        actionManager.registerActionListener(this, listOf(AACAction.PREVIEW, AACAction.EXECUTE, AACAction.HIGHLIGHT))
    }


    fun doClickHome(view: View): Boolean {
        Log.d(TAG, "doClickHome")
        aacViewModel.gotoHome()
        return true
    }


    //************************************* ICON HANDLERS ***************************************
    // handles view-based actions for all aac views (inputpageviews, etc)
    override fun handleAction(action: AACAction, data: Any?): Boolean {
        Log.d(TAG, "handleAction: $action, $data")
        return when (action) {
             AACAction.HIGHLIGHT -> (data as? View)
                ?.also { highlightIcon( it, it.tag as? IconData)}
                .let { true }
            else -> false
        }
    }

    override fun getActionTag(): Int {
        TODO("Not yet implemented")
    }

    override fun onIconEvent(icon: IconData?, action: AACAction?, view: View?) {
        when (action) {
        //    PREVIEW -> preview(icon, view)
      //      ICON_EXECUTE -> execute(icon, view)
        }
    }

    // todo: action queueing
    fun preview(list: List<IconData>) {
        list.forEach { preview(it, null)}
    }

    fun preview(icon: IconData?, v: View?) {
        Log.d(TAG, "preview icon: ${icon?.text}")
        if (((icon != null) && (v != null))  && (icon.text != DEFAULT_ICON_TEXT)) {
            highlightIcon(v, icon)
        }
    }

    fun execute(icon: IconData?, v: View?) {
        Log.d(TAG, "execute icon link?")
        icon?.linkToPageId?.also { aacViewModel.gotoPageId(it)}
    }



    fun highlightIcon(iconView: View, icon: IconData?) {
        // use textview if path is null - todo -L-
        val img = (icon ?: IconData()).thumbUri
            ?.let {uri ->
                ImageView(iconView.context)
                    .also { App.asyncSetImageBitmap(it, uri)}
            }


        val highlight = HighlightView(iconView, img, app)
        overlay?.addView(highlight)
        Handler().postDelayed(
            {
                try {
                    (highlight.parent as? ViewGroup)?.removeView(highlight)
                }
                catch (e: Exception) {
                    Log.e(TAG, "highlight timer code problem: ", e)
                }
            },
            (app.get("highlightTime")?.toString()?.toLongOrNull() ?: 1000)
        )
    }
/*
    // these don't work with new viewmodels
    fun gotoLinkIcon(icon: IconData) {
        icon.linkToPageId?.also {
            (pager?.adapter as? PageAdapter)?.setSelectionByPageId(it)
        }
    }

    override fun onPageSelected(page: PageData?) {
        Log.i(TAG, "onPageChange: " + page?.name)

        //set title
        titleView?.text = page?.name ?: "Untitled"

        //set app var:
        app.put("currentPageId", page?.id)

        // set view:

    }*/



}