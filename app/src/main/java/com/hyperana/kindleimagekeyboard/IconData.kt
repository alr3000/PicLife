package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

/**
 * Created by alr on 7/5/17.
 */
class IconData(var id: String = createIconId(),
               var index: String? = null,
               var text: String? = null,
               var fPageId: String? = null,
               var path: String? = null,
               var linkToPageId: String? = null,
                var thumbUri: Uri? = null) : ViewModel() {

    constructor(jsonObj: JSONObject) : this(
            id = jsonObj.getString("id"),
            index = jsonObj.optString("index", "nan"),
            text = jsonObj.optString("text", null),
            path = jsonObj.optString("path", null),
            linkToPageId = jsonObj.optString("linkToPageId", null)
    ) {
        thumbUri = Uri.parse(path)
    }


    // bitmap will be GC'd but not immediately (no strong references)
    // softreference seems be collected quite eagerly -- not desirable because reload slows pages!
    // LruCache - todo
    //protected var bmpRef: SoftReference<Bitmap?>? = null
    //protected var bmp: Bitmap? = null

    // todo: add link, style from data
    constructor(repository: AACRepository, liveIconResource: LiveData<Resource?>, parentPage: PageData) : this () {
        fPageId = parentPage.id
            liveIconResource.observeForever {
                updateFromResource(it)
            }
    }

    constructor(iconResource: Resource, parent: PageData?)
            : this(iconResource.uid.toString()) {
        fPageId = parent?.id
        updateFromResource(iconResource)
    }

    val TAG: String
        get() = "IconData($text)"

    // for one-time use by projections/views, etc -- not stored
    var mData: HashMap<String, String?> = hashMapOf()

    fun set(key: String, value: String?) {
        mData.put(key, value)
    }

    fun get(key: String) : String? {
        return mData.get(key)
    }

    var hasChildren: Boolean = false

    override fun toString(): String {
        return super.toString() + "($index) [$text] -> $thumbUri"
    }

    fun updateFromResource(res: Resource?) {
        text = res?.title
        id = res?.uid.toString()
        thumbUri = res?.let { Uri.parse(it.resourceUri) }
        hasChildren = res?.children?.isNotEmpty() == true
        Log.d(TAG, "onChanged")
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
            val cell = FrameLayout(context).apply { tag = icon }
            TextView(context).also {
                cell.addView(it)
                it.text = icon.text
            }
            if (withImage) {
                (icon.thumbUri)?.also { uri ->
                    if (uri.path?.isNotBlank() == true)
                        ImageView(context).also {
                            cell.addView(it)
                            App.asyncSetImageBitmap(it, uri)
                            it.scaleType = ImageView.ScaleType.FIT_XY
                        }
                }
                }

            return cell
        }
    }
}

class IconViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

}

class IconEditViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

}