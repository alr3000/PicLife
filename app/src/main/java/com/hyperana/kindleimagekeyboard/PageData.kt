package com.hyperana.kindleimagekeyboard

import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject



/**
 * Created by alr on 7/25/17.
 */
open class PageData(var id: String = createPageId(),
               var name: String? = null, var path: String? = null, var parentPageId: String? = null) {

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

    // live page resources are fetched in batch, does this even reduce the hits on database? Who knows.
    // Otherwise, maybe just give it the id and let it bind to the query here.
    constructor(repository: AACRepository, livePageResource: LiveData<Resource?>) : this () {

        live = livePageResource.apply {
            observeForever { pageRes ->

                CoroutineScope(Dispatchers.IO).launch {
                    // build icons:
                    icons = pageRes
                        ?.let { repository.getLiveChildResources(it) }
                        ?.filterNotNull()
                        ?.map { IconData(repository, it).apply { parentPageId = id } }
                        ?.toMutableList()
                        ?: mutableListOf()
                }

                // set data:
                pageRes?.uid?.let { id = it.toString() }
                name = pageRes?.title ?: "Untitled"
                Log.d(TAG, "onChanged")

            }
        }
        Log.d(TAG, "init")
    }

    val TAG : String
    get() = "PageData($name)"

    //todo: -?- not mutable, just use plus
    var icons: MutableList<IconData> = mutableListOf()



    var mData: HashMap<String, String?> = hashMapOf()

    fun set(key: String, value: String?) {
        mData.put(key, value)
    }

    fun get(key: String) : String? {
        return mData.get(key)
    }


    var live: LiveData<Resource?>? = null


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

class RecentsPage(repository: AACRepository, pageNo: Int = 0) :  PageData(
    name = "Recents" + (if (pageNo == 0) "" else "-$pageNo"),
    id = "Recents-$pageNo"
) {

    // observe recents
    init {
        //repository.getLiveRecentsPage()
    }

}

class ToolsPage(repository: AACRepository, pageNo: Int = 0): PageData(
    name = "Tools" + (if (pageNo == 0) "" else "-$pageNo"),
    id = "Tools-$pageNo"
) {
    //attach to model
}
