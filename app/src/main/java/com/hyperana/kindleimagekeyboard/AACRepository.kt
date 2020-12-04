package com.hyperana.kindleimagekeyboard

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import kotlinx.coroutines.*



class AACRepository(db: AppDatabase) : ResourceInflater {

    val TAG = "AACRepository"
    private val resourceDao = db.resourceDao()


    override fun inflatePageAsync(id: PageId?): Deferred<PageData> {
        return CoroutineScope(Dispatchers.IO).async {
            id?.let { resourceDao.find(id) }
            ?.let {
                PageData(it)
                    .apply {
                        icons = getChildIds(it).toIntArray()
                        .let { resourceDao.listAllByIds(it) }
                        .map { IconData(it, this) }
                    }
            }
                ?: PageData()
        }
    }

    override fun inflateIconAsync(id: PageId?): Deferred<IconData> {
        return CoroutineScope(Dispatchers.IO).async {
            id?.let { resourceDao.find(id) }
                ?.let { IconData(it, null) }
                ?: IconData()
        }
    }



    override fun inflateKeyboardAsync(id: PageId?): Deferred<Keyboard> {
        return CoroutineScope(Dispatchers.IO).async {
            id?.let { resourceDao.find(id) }
                ?.let {
                    Keyboard(it).apply { pageList = asyncBuildKeyboard(id)?.await() ?: emptyList() }
                }
                ?: Keyboard()
        }
    }

    fun getResourceAsync(id: PageId?) : Deferred<Resource?> {
        return CoroutineScope(Dispatchers.IO).async {
            id?.let { resourceDao.find(id) }
        }
    }

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
            if (parent.resourceType == Resource.Type.PAGE.name || children.count() > 0) {

                val page = PageData(parent)
                pages.add(page)

                // add all children of children recursively to pagelist:
                children.map { async { requestPagesRecursive(it) } }
                    .awaitAll()
                    .also { pages.addAll(it.flatten()) }

                // add children to this page as icons:
                children.map { IconData(it, page) }
            }

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