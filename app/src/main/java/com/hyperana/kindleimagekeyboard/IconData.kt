package com.hyperana.kindleimagekeyboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import org.json.JSONObject
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

/**
 * Created by alr on 7/5/17.
 */
class IconData(val id: String = createIconId(),
               var index: String? = null,
               var text: String? = null,
               var pageId: String? = null,
               var path: String? = null,
               var linkToPageId: String? = null) {

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
        json.put("pageId", pageId)
        json.put("index", index.toString())
        json.put("linkToPageId", linkToPageId)
        return json
    }


    // var appdataFilename: String? = null
}
