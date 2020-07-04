package com.hyperana.kindleimagekeyboard

import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

object PageAdapterFactory {
    val TAG = "PageAdapterFactory"

    fun create(
        app: App,
        pages: List<PageData> = listOf()
    ) : BaseAdapter {
        return object: BaseAdapter() {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                try {
                    Log.d(TAG, "pager adapter getView: " + position)

                    //todo: -L- page type determined by preference: infinite scroll, expandable, fixed
                    // reuse old pageView if available
                    // if any page setting had changed, this whole thing would be rebuilt, so they
                    // should all be reusable

                    val cv = (convertView as? InputPageView)
                    return if (cv != null) cv.refit(getItem(position) as PageData)!!
                    else InputPageView(
                        parent!!.context,
                        getItem(position) as PageData,
                        Color.parseColor(app.get("backgroundColor")?.toString())
                    )
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
    }
}