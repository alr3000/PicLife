package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.*

class AACManager (
    val app: App,
    val overlay: ViewGroup?,
    val pager: SwipePagerView?,
    val input: InputViewController?,
    val speaker: Speaker?,
    val accessSettings: AccessSettingsController?,
    val gotoHomeView: View?,
    val titleView: TextView?
)
    : IconListener, SwipePagerView.PageListener
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
        pager?.adapter = PageAdapterFactory.create(app, pages, this)

        pager?.pageListener = this

    }

    fun setCurrentPage(pageId: String?) {
        Log.d(TAG, "setCurrentPage -- id: $pageId / ${pager?.adapter?.count}")

        pageId?.also { pager?.setPageById(it)}
            ?:    pager?.setPage(0) // first in list by default
    }


    // icon interface:
    override fun preview(icon: IconData?, v: View?) {
        Log.d(TAG, "preview icon")
        if (((icon != null) && (v != null))  && (icon.text != DEFAULT_ICON_TEXT)) {
            highlightIcon(v, icon)
        }
    }

    override fun execute(icon: IconData?, v: View?) {
        Log.d(TAG, "execute icon")
        if ((icon != null) && (icon.text != DEFAULT_ICON_TEXT)) {
            typeIcon(icon)
            gotoLinkIcon(icon)
        }
    }




    //Put text into input on text icon touch
    fun typeIcon(icon: IconData) {
        Log.d(TAG, "typeIcon: " + icon.text)
        try {
            val typeLinks = app.get("doTypeLinks")?.toString()?.toBoolean() ?: false
            if ((icon.linkToPageId != null) && (!typeLinks)) {
                return
            }
            if (icon.text == null) {
                return
            }
           input?.inputter?.input(icon.text!!)
        } catch(e: Exception) {
            Log.e(TAG, "problem with icon input", e)
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