package com.hyperana.kindleimagekeyboard

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AACRepository(db: AppDatabase) {

    val TAG = "PageRepository"
    private val resourceDao = db.resourceDao()


    // LiveData:
    suspend fun getLiveResource(id: Int) : LiveData<Resource?>? {
        resourceDao.getLive(id)?.also {
            if (it.value != null) return it
        }
        return null
    }
    suspend fun getLiveChildResources(res: Resource) : List<LiveData<Resource?>?> {
        return getChildIds(res)
            .map { resourceDao.getLive(it) }
            .also { Log.d(TAG, "getLiveChildResources: ${it.size} items")}
    }
    suspend fun getLiveDefault(type: Resource.Type) : LiveData<Resource?>? {
        return resourceDao.getLiveAny(type.name)
            .also { Log.d(TAG, "default resource: ${it?.value?.title}")}
    }
    suspend fun getLiveListKeyboards() : LiveData<List<Resource>?>? {
        return resourceDao.getAllLiveByType(Resource.Type.KEYBOARD)
    }


    // lists:
    fun getChildIds(res: Resource) : List<Int> {
        return res.children.split(AppDatabase.DELIMITER)
            .map { it.toIntOrNull() }
            .filterNotNull()
    }

    suspend fun listKeyboards() : List<Resource>? {
        return resourceDao.listAllByType(Resource.Type.KEYBOARD.name)
            .also { Log.d(TAG, "listKeyboards: ${it.joinToString()}")}
    }


}