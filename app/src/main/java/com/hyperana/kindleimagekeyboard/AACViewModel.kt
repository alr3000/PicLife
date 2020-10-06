package com.hyperana.kindleimagekeyboard

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import java.lang.Math.min

interface PageNavigator {
    fun goUp(num: Int = 1)
    fun goDown(num: Int = 1)
    fun goLeft(num: Int = 1)
    fun goRight(num: Int = 1)
    fun gotoPageId(id: String)
    fun gotoHome()
    fun gotoAACPages()
    fun goBack()
    fun goForward()
}



// todo: make a factory to add projection, defaults, keyboard, recents, tools
// This model holds the keyboard, aac pages, recents pages, and tools pages,
// it handles the logic of the AAC page/keyboard navigation and "exports" the page currently being requested/viewed:
class AACViewModel(application: Application) : AndroidViewModel(application), PageNavigator, Observer<Resource?> {

    val TAG = "AACViewModel${hashCode()}"

    // saved data:
    val EXTRA_KEYBOARD_ID = "keyboard_id"
    val EXTRA_PAGE_ID = "page_id"

    val app = App.getInstance(application.applicationContext)
    val repository = AACRepository(AppDatabase.getDatabase(application.applicationContext)!!)

    // page map for navigation:

    //todo: could be a live transformation of the keyboard
    //todo: use transformations to observe data on other model layers
    private var aacPageList : List<PageData> = listOf()
        set(value) {
            field = value
            Log.i(TAG, "setAACPageList with ${value.size} pages")

            // nav to new position of current page:
            liveCurrentPage.value?.baseId
                ?.let { getPositionOfPageId(it)}
                ?.also { Log.d(TAG, "found current page in new pages")}
                ?.also { setPosition(it) }
                ?: gotoHome()
        }
    //get() { return getProjectedPages(field) }

    private var upList = (0 .. 4).map { RecentsPage(repository, it)}
    private var downList = (0..2).map { ToolsPage(repository, it) }
    // position between up and down list where aac pages "show through"
    private val rest: Int
        get() = downList.size

    //todo: this could be recents, back/forward buttons most likely shown in recents page
    val backList: MutableList<String> = mutableListOf()


    // State:
    // create projected pages (and within them, icons) from the keyboard's child list:
    override fun onChanged(t: Resource?) {
        Log.i(TAG, "live resource change: ${t?.resourceType}")



        // if the keyboard or page contents change, the affected models will be replaced here.
        when (t?.resourceType) {

            Resource.Type.KEYBOARD.name -> {
                // access database in background to get child pages, observe them here:
                CoroutineScope(Dispatchers.IO).launch {

                    t
                        .let { repository.getLiveChildResources(it) }
                        .filterNotNull()

                        .mapIndexed { index, liveData ->
                            CoroutineScope(Dispatchers.Main).async {
                                liveData.observeForever(this@AACViewModel)
                            }
                        }
                }

                // set pagelist with stubs:
                aacPageList = repository
                    .getChildIds(t)
                    .map { PageData(it.toString()) }

                // get default page (icons may not be there yet)
                gotoHome()
            }

            Resource.Type.PAGE.name ->
                if (aacPageList.any { it.baseId == t.uid.toString()}) {

                        val newPage = PageData(t)

                        // icons observe their own models for changes:
                        CoroutineScope(Dispatchers.IO).launch {

                            repository
                                .getLiveChildResources(t)
                                .filterNotNull()
                                .map {
                                    CoroutineScope(Dispatchers.Main).async {
                                        IconData(repository, it, newPage)
                                    }
                                }
                                .awaitAll()
                                .also {
                                    Log.i(TAG, "setting ${it.size} icon stubs for page ${newPage.name}")
                                    newPage.icons = it
                                    refreshPage(newPage)
                                }
                        }
                    }
        }
    }


    fun refreshPage(new: PageData) {
        Log.d(TAG, "find page ${new.baseId} in ${aacPageList.map { it.baseId }}")
        val start = aacPageList.indexOfFirst{ it.baseId == new.baseId }
        val end = aacPageList.indexOfLast { it.baseId == new.baseId}
        if (start >= 0 && end >= 0)
        aacPageList = aacPageList
            .slice(0 until start)
            .plus (getProjectedPages(listOf(new)))
            .plus (
                aacPageList.slice( min(end + 1, aacPageList.size) until aacPageList.size)
            )
            .also { Log.i(TAG, "page ${new.name}(${new.baseId}) refreshed")}
        else Log.i(TAG, "requested page not found in list")
    }




    private var liveKeyboardResource: LiveData<Resource?>? = null
        set(value) {
            Log.i(TAG, "setLiveKeyboard: $value")
            CoroutineScope(Dispatchers.Main).launch {
                field?.removeObserver(this@AACViewModel)
                field = value
                field?.observeForever(this@AACViewModel)
            }
        }

    private var currentPageLiveData = MutableLiveData(PageData())
    var liveCurrentPage: LiveData<PageData> = currentPageLiveData

    private var currentPosition: Position = Pair(0,0)
    private val shouldShowAlt: Boolean
        get() = (currentPosition.second != rest)

    private val currentPageId: Int?
        get() = currentPageLiveData.value?.id?.toIntOrNull()


    // set keyboard and current page from saved or preferences if out-of-whack
    fun onRestoreInstanceState(savedInstanceState: Bundle?, prefs: SharedPreferences) {
        Log.d(TAG, "restore instance state")
        val prefId = prefs.getInt("currentKeyboardId", -1)
        savedInstanceState.also { saved ->
            (saved?.getInt(PREF_KEYBOARD_ID, prefId) ?: prefId)
                .also { id ->
                    if (id != liveKeyboardResource?.value?.uid)
                        replaceKeyboard(id)
                }
            saved?.getInt(EXTRA_PAGE_ID, -1)?.also { if (it != -1 && it != currentPageId) gotoPageId(it.toString()) }
        }
    }

    // set keyboard by id, or if not found, default:
    fun replaceKeyboard(id: Int)  {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "replace keyboard")
            liveKeyboardResource = repository.getLiveResource(id)
                ?.let { if (it.value != null) it else null }
                ?: repository.getLiveDefault(Resource.Type.KEYBOARD)
        }
    }

    // store current keyboard id, page id
    fun onSaveInstanceState(outState: Bundle?) : Bundle {
        return (outState ?: Bundle()).also { saved ->
            (liveKeyboardResource?.value?.uid ?: -1).also { saved.putInt(EXTRA_KEYBOARD_ID, it)  }
            (currentPageLiveData.value?.id?.toIntOrNull() ?: -1).also { saved.putInt(EXTRA_PAGE_ID, it)}
        }
    }

    // Page Navigation:
    override fun goBack() {

    }

    override fun goForward() {

    }

    override fun goUp(num: Int) {
        val newPos = getIndexFixed(upList.size + 1 + downList.size, currentPosition.second + num)
        //if (newPos != currentPosition.first)
        setPosition(Pair(currentPosition.first, newPos))
    }
    override fun goDown(num: Int) { goUp( num * -1 ) }

    // selects next view in main axis IFF alt is at rest:
    override fun goLeft(num: Int) {
        if (shouldShowAlt) return
        val newPos = getIndexLooping(aacPageList.size, currentPosition.first + num)
        //if (newPos != currentPosition.first)
        setPosition(Pair(newPos, currentPosition.second))
    }
    override fun goRight(num: Int) { goLeft(num * -1) }

    // set current page, notify observers:
    // don't move if no page at that position or same page--
    fun setPosition(newPos: Position) {
        Log.i(TAG, "setPosition: $newPos")
        val currentId = currentPageId.toString()
        getPageFromPosition(newPos)?.also { newPage ->
           // if (newPage.id == currentId) return
            currentPosition = newPos
            currentPageLiveData.postValue(newPage)
        }
    }

    // find position in keyboard
    // add current page to backlist beforehand if required (if this is not a "backpress"):
    //        backList.add(currentPageId.toString())
    //
    // as it is likely a jump (not paging scroll)
    // todo: or, goto new keyboard:
    override fun gotoPageId(id: String) {
        getPositionOfPageId(id)?.also { setPosition(it) }
            ?: Log.w(TAG, "requested page id: $id not found in current keyboard")
    }

    override fun gotoHome() {
        setPosition(Pair(0, rest))
    }

    override fun gotoAACPages() {
        setPosition(Pair(currentPosition.first, rest))
    }


    fun getIndexLooping(length: Int, pos: Int) : Int {
        return if (length == 0) 0 else (pos + length)%length
    }

    fun getIndexFixed(length: Int, pos: Int) : Int {
        return pos.coerceIn(0, length)
    }

    fun getPageFromPosition(pos: Position) : PageData? {
        return when {
            // show aac page from pos.first:
            (pos.second == rest) -> aacPageList.getOrNull(pos.first) ?: PageData()

            // show up/down page from pos.second:
            pos.second == rest -> null
            pos.second < rest -> downList.getOrNull(Math.abs(pos.second - rest) - 1)
            else -> upList.getOrNull((pos.second - rest) - 1)
        }
    }

    fun getPositionOfPageId(id: String) : Position? {
        fun compare(str: String, page: PageData) =
            str == page.id || str == page.baseId

        // try up/down lists first:
        downList.plus (PageData()).plus(upList)
            .indexOfFirst { compare(id, it) }
            .also { if (it != -1)
                return Pair(currentPosition.first, it)
            }

        aacPageList.indexOfFirst {
            Log.d(TAG, "found id ${it.baseId}")
            compare(id, it)
        }.also {
            if (it != -1)
                return Pair(it, currentPosition.second)
        }
        return null
    }

    fun getProjectedPages(original: List<PageData>) : List<PageData> {
        val app = App.getInstance(getApplication<Application>().applicationContext)
        return original
            /*.let {
                RemoveBlanksProjection("icon").project(it)
            }
            .let {
                LinkedPagesProjection(app.get("createLinks").toString()).project(it)
            }*/
            .let {
                val cols = app.get("columns").toString().toInt()
                val rows = resolveRows(cols, app.appContext.resources.getConfiguration().orientation)

                FittedGridProjection(
                    cols = cols,
                    rows = rows,
                    margins = app.get("iconMargin").toString().toIntOrNull()
                ).project(it)
            }
    }

    fun resolveRows(cols: Int, orientation: Int) : Int {
        val rToC = if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            0.5 else 2.0

        return kotlin.math.round(cols.toDouble() * rToC).toInt()
    }


}