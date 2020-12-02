package com.hyperana.kindleimagekeyboard

import android.util.Log
import androidx.lifecycle.LiveData
import java.util.*

/**
 * Created by alr on 12/1/17.
 */
class Keyboard()  {

    var id: PageId = -1

    constructor(liveResource: LiveData<Resource?>?) : this() {

        // auto-updated when underlying resource data is updated
        liveResource?.observeForever {
            Log.i(TAG, "onObserve keyboard (${it?.uid}")
            update(it)
        }
    }
    val TAG = "Keyboard"


    fun update(res: Resource?) {
        id = res?.uid ?: -1
    }



}