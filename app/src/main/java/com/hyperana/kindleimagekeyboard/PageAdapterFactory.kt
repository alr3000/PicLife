package com.hyperana.kindleimagekeyboard

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter

open class PageAdapter (
    val pages: List<PageData> = listOf(),
    listener: PageSelectionListener? = null
) : BaseAdapter() {

  open val TAG = "PageAdapter"

    open var pageListener: PageSelectionListener? = listener
    private var selectedPosition: Int? = null






    // this adapter does not handle view insertion.
    // listen for select and do it in ViewGroup class:
    interface PageSelectionListener {
        fun onPageSelected(page: PageData?)
    }

    val defaultItem = PageData().apply { name = "No Pages Available" }

    open fun getAllItems() : List<PageData> = pages


    // adapter only officially ever has one page, any page requested will either be
    // selected page or default page
    override fun getCount(): Int {
        return getAllItems().count()
    }

   override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        try {
            val newPage = getItem(position) as PageData
            Log.i(TAG, "pager adapter getView($position) for ${newPage.name}")


            val context = parent!!.context
            return  ((convertView as? InputPageView)
                ?: InputPageView(context)).apply {
                page = newPage
            }

        }
        catch (e: Exception) {
            Log.e(TAG, "could not get view at " + position, e)
            return View(parent!!.context)
        }

    }



    override fun getItem(position: Int): Any {
        return pages.getOrNull(position)  ?: defaultItem
    }

    override fun getItemId(position: Int): Long {
        return 0 // not implemented
    }


    // selectable interface:
    open fun getSelectedItemPosition(): Int? {
        return selectedPosition
    }

    open fun getSelectedItem(): Any? {
        return getSelectedItemPosition()?.let { getItem(it) }
    }

    open fun getSelectedItemId(): Long {
        return getSelectedItemPosition()?.let { getItemId(it) } ?: 0L
    }

    open fun getSelectedView(parent: ViewGroup, convertibleView: InputPageView?) : View {
        return (selectedPosition ?: 0).let { pos -> getView(pos, convertibleView, parent) }
    }

    open fun setSelection(position: Int?) {
        Log.d(TAG, "setSelection: $position")

        // do nothing if request is out of range
        //if (position?.let { it in 0 until count} == false) return

        // loop if out of range:
        selectedPosition = position?.let { Math.abs(it).rem(count) }
        pageListener?.onPageSelected(getSelectedItem() as? PageData)

    }

    open fun setSelectionByPageId(id: String) {
        getAllItems().indexOfFirst { it.id == id }
            .also { setSelection(if (it != -1) it else null) }
    }

}
