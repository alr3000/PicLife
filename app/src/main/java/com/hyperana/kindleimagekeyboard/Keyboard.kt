package com.hyperana.kindleimagekeyboard

import android.util.Log
import androidx.lifecycle.LiveData
import java.util.*

/**
 * Created by alr on 12/1/17.
 */
class Keyboard()  {

    var id: PageId = -1

    var pageList: List<PageData> = emptyList()
    var homepageId: PageId? = null

    constructor(resource: Resource?) : this() {


            Log.i(TAG, "init keyboard ($resource")
            update(resource)
    }
    val TAG = "Keyboard"


    fun update(res: Resource?) {
        id = res?.uid ?: -1

        val data = res?.let { Resource.ExtractData(it.data) } ?: mapOf()
        homepageId = data.get(KEY_HOMEPAGE)?.toInt()
    }


    companion object {
        const val KEY_HOMEPAGE = "homepage_id"
    }


}