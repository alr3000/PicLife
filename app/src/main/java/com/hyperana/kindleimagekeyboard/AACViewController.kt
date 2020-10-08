package com.hyperana.kindleimagekeyboard

import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.*

// handles aac icon actions, such as deep links (not swipe paging) and overlay graphics
// handles view updates outside of the pager (title, back)
class AACViewController (
    val app: App,
    val overlay: ViewGroup?,
    val lifecycleOwner: LifecycleOwner,
    val aacViewModel: AACViewModel,
    val aacToolbar: ActionToolbar?,
    val actionManager: ActionManager
)
    :    ActionListener
{

    val TAG = "AACViewController"
    var title = AACAction("AACPageTitle", "")
    val back = AACAction("AACBack", "Back", android.R.drawable.ic_media_previous)
    val home = AACAction.HOME
    val settings = AACAction.OPEN_SETTINGS

    init {

        // set aac context actions in toolbar:
            aacViewModel.liveCurrentPage.observe(lifecycleOwner) {
                title = AACAction("AACPageTitle", it.name ?: "")
               updateToolbar()
            }

        // register for actions generated elsewhere:
        actionManager.registerActionListener(this, listOf(AACAction.HIGHLIGHT))
    }

    fun updateToolbar() {
        aacToolbar?.replaceActions(this,
            listOf(title, settings)
        )
        if (app.get("doHomeButton")?.toString()?.toBoolean() ?: true)
            aacToolbar?.setLeftCornerAction(this, home)
    }


    //************************************* ICON HANDLERS ***************************************
    // handles view-based actions for all aac views (inputpageviews, etc)
    override fun handleAction(action: AACAction, data: Any?): Boolean {
        Log.d(TAG, "handleAction: $action, $data")
        return when (action) {
            AACAction.HOME -> aacViewModel.gotoHome().let { true }
            AACAction.HIGHLIGHT -> (data as? View)
                ?.also { highlightIcon( it, it.tag as? IconData)}
                .let { true }
            else -> actionManager.handleAction(action, data)
        }
    }

    override fun getActionTag(): Int {
        return TAG.hashCode()
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