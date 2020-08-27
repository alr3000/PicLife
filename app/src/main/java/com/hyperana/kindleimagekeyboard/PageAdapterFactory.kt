package com.hyperana.kindleimagekeyboard

import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

open class PageAdapter (
    val pages: List<PageData> = listOf()
) : BaseAdapter() {

  open val TAG = "PageAdapter"

    open fun getAllItems() : List<PageData> = pages

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        try {
            val newPage = getItem(position) as PageData
            Log.i(TAG, "pager adapter getView($position) for ${newPage.name}")


            val cv = (convertView as? InputPageView)
            return if (cv != null) cv.refit(getItem(position) as PageData)!!
            else InputPageView(parent!!.context).apply {
                // todo: attach viewholder w/ model instead
                page = newPage
                color = Color.RED
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "could not get view at " + position)
            return View(parent!!.context)
        }

    }

    override fun getItem(position: Int): Any {
        return pages.get(position)
    }

    override fun getItemId(position: Int): Long {
        return 0 // not implemented
    }

    override fun getCount(): Int {
        return pages.count()
    }
}
