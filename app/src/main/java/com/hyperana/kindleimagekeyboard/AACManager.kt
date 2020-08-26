package com.hyperana.kindleimagekeyboard

import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData

class AACManager (
    val app: App,
    val overlay: ViewGroup?,
    val pager: SwipePagerView?,
    val gotoHomeView: View?,
    val titleView: TextView?
)
    :  SwipePagerView.PageListener, IconListener
{

    val TAG = "AACManager"


    init {
        gotoHomeView?.apply {
            visibility =
                if (app.get("doHomeButton")?.toString()?.toBoolean() ?: true) View.VISIBLE
                else View.INVISIBLE

            setOnClickListener { v -> doClickHome(v) }
        }
    }


    fun setPages(pages: List<PageData>) {
        Log.d(TAG, "setPages: ${pages.count()}")
        // build a view with iconListener for each page that has icons
        // uses columns, iconMargin, createLinks
        pager?.adapter = PageAdapterFactory.create(app, pages)
        pager?.pageListener = this
    }

    fun setToolPages(pages: List<PageData>) {
        pager?.verticalAdapter = PageAdapterFactory.create(app, pages)
        pager?.verticalPageListener = this
    }

    fun setCurrentPage(pageId: String?) {
        Log.d(TAG, "setCurrentPage -- id: $pageId / ${pager?.adapter?.count}")

        pageId?.also { pager?.setPageById(it)}
            ?:    pager?.setPage(0) // first in list by default
    }


    // icon interface:
    override fun onIconEvent(icon: IconData?, action: AACAction?, view: View?) {
        when (action) {
            AACAction.ICON_PREVIEW -> preview(icon, view)
            AACAction.ICON_EXECUTE -> execute(icon, view)
        }
    }

    fun preview(icon: IconData?, v: View?) {
        Log.d(TAG, "preview icon")
        if (((icon != null) && (v != null))  && (icon.text != DEFAULT_ICON_TEXT)) {
            highlightIcon(v, icon)
        }
    }

    fun execute(icon: IconData?, v: View?) {
        Log.d(TAG, "execute icon link?")
        if ((icon != null) && (icon.text != DEFAULT_ICON_TEXT)) {
            gotoLinkIcon(icon)
        }
    }


    fun highlightIcon(iconView: View, icon: IconData) {
        // use textview if path is null - todo -L-
        val img =
            if (icon.path != null) {
                ImageView(iconView.context).also {
                    App.asyncSetImageBitmap(it, icon.path!!)
                }
            }
            else null

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


    fun doClickHome(view: View): Boolean {
        Log.d(TAG, "doClickHome")
        gotoPage(0)
        return true
    }



    //************************************* ICON HANDLERS ***************************************

    fun gotoLinkIcon(icon: IconData) {
        if (icon.linkToPageId == null) {
            return
        }
        val newPageIndex = app.getPageList().indexOfFirst { it.id == icon.linkToPageId }
        if (newPageIndex < 0) {
            Log.w(TAG, "linked page not found for " + icon.text + "->" + icon.linkToPageId)
            return
        }
        gotoPage(newPageIndex)
    }

    fun gotoPage(index: Int) {
        try {
            pager?.setPage(index)
        }catch (e: Exception) {
            Log.e(TAG, "go to page fail", e)
        }
    }
    override fun onPageChange(page: PageData?, index: Int) {
        Log.d(TAG, "onPageChange: " + page?.name)

        //set title
        titleView?.text = page?.name ?: "Untitled"

        //set app var:
        app.put("currentPageId", page?.id)
    }



}