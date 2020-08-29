package com.hyperana.kindleimagekeyboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

interface PageNavigator {
    fun goUp(num: Int = 1)
    fun goDown(num: Int = 1)
    fun goLeft(num: Int = 1)
    fun goRight(num: Int = 1)
    fun gotoPageId(id: String)
    fun gotoHome()
    fun gotoAACPages()
}

class AACViewModel(app: Application) : AndroidViewModel(app), PageNavigator {

    val TAG = "AACViewModel"

    // observed data from repository: ordered lists of page resources
    val repository = PageRepository(AppDatabase.getDatabase(app.applicationContext)!!)
    private var aacPageList : List<PageData> = listOf(PageData())
    private var upList = repository.recentsPages
    private var downList = repository.toolsPages

    init {
        repository.aacPageResources?.observeForever {
            aacPageList = it?.map {
                resourceToPage(it)
            } ?: listOf(PageData())
        }
    }

    // convert resource to PageData:
    fun resourceToPage(res: Resource) : PageData {
        return PageData(name = res.title)
    }

    // This model's observable data: the current page
    private var currentPageLiveData =  MutableLiveData<PageData>(PageData())
    val currentPage: LiveData<PageData>
        get() = currentPageLiveData


    private var currentPosition: Position = Pair(0,0)
    private val shouldShowAlt: Boolean
        get() = (currentPosition.second != downList.size)

    // position between up and down list where aac pages "show through"
    private val rest: Int
        get() = downList.size

    // Page Navigation:
    override fun goUp(num: Int) {
        val newPos = getIndexFixed(upList.size + 1 + downList.size, currentPosition.second)
        if (newPos != currentPosition.first)
            setPosition(Pair(currentPosition.first, newPos))
    }
    override fun goDown(num: Int) { goUp( num * -1 ) }

    // selects next view in main axis IFF alt is at rest:
    override fun goLeft(num: Int) {
        if (shouldShowAlt) return
        val newPos = getIndexLooping(aacPageList.size, currentPosition.first + num)
        if (newPos != currentPosition.first)
            setPosition(Pair(newPos, currentPosition.second))
    }
    override fun goRight(num: Int) { goLeft(num * -1) }

    // set current page, notify observers:
    fun setPosition(newPos: Position) {
        currentPosition = newPos
        currentPageLiveData.value = getPageFromPosition(currentPosition)
    }

    override fun gotoPageId(id: String) {
        getPositionOfPageId(id)?.also { setPosition(it) }
    }

    override fun gotoHome() {
        setPosition(Pair(0, rest))
    }

    override fun gotoAACPages() {
        setPosition(Pair(currentPosition.first, rest))
    }


    fun getIndexLooping(length: Int, pos: Int) : Int {
        return (pos + length)%length
    }

    fun getIndexFixed(length: Int, pos: Int) : Int {
        return pos.coerceIn(0, length)
    }

    fun getPageFromPosition(pos: Position) : PageData {

        // show aac page from pos.first:
        return if (pos.second == rest) aacPageList.getOrNull(pos.first) ?: PageData()

        // show up/down page from pos.second:
        else when {
            pos.second == rest -> null
            pos.second < rest -> downList.getOrNull(Math.abs(pos.second - rest) - 1)
            else -> upList.getOrNull((pos.second - rest) - 1)
        } ?: PageData()
    }

    fun getPositionOfPageId(id: String) : Position? {
        // try up/down lists first:
        downList.plus (PageData()).plus(upList)
            .indexOfFirst { it.id == id }
            .also { if (it != -1)
                return Pair(currentPosition.first, it)
            }

        aacPageList.indexOfFirst { it.id == id }.also {
            if (it != -1)
                return Pair(it, currentPosition.second)
        }
        return null
    }
}