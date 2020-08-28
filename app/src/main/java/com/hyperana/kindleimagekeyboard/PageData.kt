package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.JsonWriter
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject


open class PageViewHolder(var page: PageData, view: InputPageView) : RecyclerView.ViewHolder(view) {

}

/**
 * Created by alr on 7/25/17.
 */
open class PageData(val id: String = createPageId(),
               var name: String? = null, var path: String? = null, var parentPageId: String? = null)
    {

    constructor(jsonObj: JSONObject) : this(
            id = jsonObj.getString("id"),
            name = jsonObj.optString("name", null),
            path =  jsonObj.optString("path", null),
            parentPageId = jsonObj.optString("parentPageId", null)
    ) {

        icons = getListFromJSONArray(jsonObj.getJSONArray("icons"))
                .map { IconData(it) }
                .toMutableList()

    }


    //todo: -?- not mutable, just use plus
    var icons: MutableList<IconData> = mutableListOf()



    var mData: HashMap<String, String?> = hashMapOf()

    fun set(key: String, value: String?) {
        mData.put(key, value)
    }

    fun get(key: String) : String? {
        return mData.get(key)
    }


    //TODO -L- add page title? for editable name

    class EditViewHolder(view: ViewGroup) {
        var name: EditText? = null
        var iconCount: TextView? = null

        init {
            name = view.findViewWithTag("pageName") as EditText
            iconCount = view.findViewWithTag("iconCount") as TextView
        }
    }

    // put/retrieve all data as strings to preserve null integers. Null data left out
    fun toJSONObject(page: PageData = this) : JSONObject {
        val json: JSONObject = JSONObject()
        json.put("id", page.id)
        json.putOpt("name", page.name)
        json.putOpt("path", page.path)
        json.putOpt("parentPageId", page.parentPageId)

        val iconArray = JSONArray()
        page.icons.onEach { iconArray.put(it.toJSONObject())}
        json.put("icons", iconArray)

        return json
    }


}

class RecentsPage :  PageData(name = "Recents") {
    //attach to model
}

class ToolsPage: PageData(name = "Tools") {
    //attach to model
}
