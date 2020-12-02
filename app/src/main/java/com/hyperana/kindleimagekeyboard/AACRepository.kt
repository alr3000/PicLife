package com.hyperana.kindleimagekeyboard

import android.database.Cursor
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.*

class AACRepository(db: AppDatabase) {

    val TAG = "AACRepository"
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
        return resourceDao.getAllLiveByType(Resource.Type.KEYBOARD.name)
    }


    // lists:
    fun getChildIds(res: Resource) : List<Int> {
        return res.children.split(AppDatabase.DELIMITER)
            .map { it.toIntOrNull() }
            .filterNotNull()
    }

    fun getAllById(ids: IntArray) : List<Resource> {
        return resourceDao.listAllByIds(ids)
    }

    suspend fun listKeyboards() : List<Resource>? {
        return resourceDao.listAllByType(Resource.Type.KEYBOARD.name)
            .also { Log.d(TAG, "listKeyboards: ${it.joinToString()}")}
    }

    // this can't be a local function inside asyncBuildKeyboard due to Kotlin compiler issue:
    suspend fun requestPagesRecursive(parent: Resource): List<PageData> {
        return supervisorScope {

            val pages = mutableListOf<PageData>()

            // get child resources:
            val children = resourceDao.listAllByIds(getChildIds(parent).toIntArray())

            // add parent to pagelist if it is a page resource or has children:
            if (parent.resourceType == Resource.Type.PAGE.name || children.count() > 0)
                pages.add(PageData(parent))


            // add all children of children recursively to pagelist:
            children.map { async { requestPagesRecursive(it) } }
                .awaitAll()
                .also { pages.addAll(it.flatten()) }

            // add children to this page as icons:
            children.map { IconData(it, parent.uid.toString()) }


            pages
        }
    }



    suspend fun asyncBuildKeyboard(id: Int?) : Deferred<List<PageData>>? {
        return supervisorScope {

            // get requested keyboard, null if not found:
             id
                ?.let { resourceDao.find(it) }
                ?.let {

                    // request child resources:
                    async { requestPagesRecursive(it) }

                }
        }
    }



}