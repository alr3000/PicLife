package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import org.json.JSONObject

/**
 * Created by alr on 7/5/17.
 */
class IconData(val id: String = createIconId(),
               var index: String? = null,
               var text: String? = null,
               var fPageId: String? = null,
               var path: String? = null,
               var linkToPageId: String? = null)  {

    constructor(jsonObj: JSONObject) : this(
            id = jsonObj.getString("id"),
            index = jsonObj.optString("index", "nan"),
            text = jsonObj.optString("text", null),
            path = jsonObj.optString("path", null),
            linkToPageId = jsonObj.optString("linkToPageId", null)
    )

    // bitmap will be GC'd but not immediately (no strong references)
    // softreference seems be collected quite eagerly -- not desirable because reload slows pages!
    // LruCache - todo
    //protected var bmpRef: SoftReference<Bitmap?>? = null
    //protected var bmp: Bitmap? = null


    // for one-time use by projections/views, etc -- not stored
    var mData: HashMap<String, String?> = hashMapOf()

    fun set(key: String, value: String?) {
        mData.put(key, value)
    }

    fun get(key: String) : String? {
        return mData.get(key)
    }




    // put/retrieve all data as strings to preserve nulls
    fun toJSONObject(): JSONObject {
        val json =  JSONObject()
        json.put("id", id)
        json.put("text", text)
        json.put("path", path)
        json.put("pageId", fPageId)
        json.put("index", index.toString())
        json.put("linkToPageId", linkToPageId)
        return json
    }


    // var appdataFilename: String? = null

    companion object {
        fun createView(icon: IconData, context: Context, withImage: Boolean = true) : View {
            val cell = FrameLayout(context)
            TextView(context).also {
                cell.addView(it)
                it.text = icon.text
            }
            if (withImage && icon.path != null) {
                ImageView(context).also {
                    cell.addView(it)
                    App.asyncSetImageBitmap(it, icon.path!!)
                    it.scaleType = ImageView.ScaleType.FIT_XY
                }
            }
            return cell
        }
    }
}
