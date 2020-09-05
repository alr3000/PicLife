package com.hyperana.kindleimagekeyboard

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface PageNavigator {
    fun goUp(num: Int = 1)
    fun goDown(num: Int = 1)
    fun goLeft(num: Int = 1)
    fun goRight(num: Int = 1)
    fun gotoPageId(id: String)
    fun gotoHome()
    fun gotoAACPages()
    fun goBack()
}


// todo: make a factory to add projection, defaults, keyboard, recents, tools
// This model holds the keyboard, aac pages, recents pages, and tools pages,
// it handles the logic of the AAC page/keyboard navigation and "exports" the page currently being requested/viewed:
class AACViewModel(application: Application) : AndroidViewModel(application), PageNavigator {

    val TAG = "AACViewModel${hashCode()}"

    // saved data:
    val EXTRA_KEYBOARD_ID = "keyboard_id"
    val EXTRA_PAGE_ID = "page_id"

    val app = App.getInstance(application.applicationContext)
    val repository = AACRepository(AppDatabase.getDatabase(application.applicationContext)!!)

    // page map for navigation:
    private var aacPageList : List<PageData> = listOf()
    set(value) {
        field = value
        Log.i(TAG, "setAACPageList with ${value.size} pages")
    }
    private var upList = listOf(RecentsPage())
    private var downList = listOf(ToolsPage())
    // position between up and down list where aac pages "show through"
    private val rest: Int
        get() = downList.size

    // State:
    // create projected pages (and within them, icons) from the keyboard's child list:
    val keyboardObserver = object: Observer<Resource?> {
        override fun onChanged(t: Resource?) {
            Log.i(TAG, "live keyboard change")
            CoroutineScope(Dispatchers.IO).launch {
                aacPageList = (t
                    ?.let { repository.getLiveChildResources(it) }
                    ?.filterNotNull()
                    ?.map { PageData(repository, it) }
                    ?: listOf(PageData()))

                    // create indices for icons, overflow pages, etc!
                    .let { getProjectedPages(it) }

                //todo: find current page if it exists in new keyboard, else

                // get default page
                gotoHome()
            }
        }
    }


    var liveKeyboardResource: LiveData<Resource?>? = null
    set(value) {
        field?.removeObserver(keyboardObserver)
        field = value
        field?.observeForever(keyboardObserver)
    }
    private var currentPageLiveData = MutableLiveData(PageData())
    var liveCurrentPage: LiveData<PageData> = currentPageLiveData

    private var currentPosition: Position = Pair(0,0)
    private val shouldShowAlt: Boolean
        get() = (currentPosition.second != rest)

    private val currentPageId: Int?
        get() = currentPageLiveData.value?.id?.toIntOrNull()


    // viewmodels created on the main thread
    init {
        Log.i(TAG, "init")
    }

    // set keyboard and current page from saved or preferences if out-of-whack
    fun onRestoreInstanceState(savedInstanceState: Bundle?, prefs: SharedPreferences) {
        Log.d(TAG, "restore instance state")
        val prefId = prefs.getInt("currentKeyboardId", -1)
        savedInstanceState.also { saved ->
            (saved?.getInt(EXTRA_KEYBOARD_ID, prefId) ?: prefId)
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
        //todo: use recents
        TODO("Not yet implemented")
    }

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
    // don't move if no page at that position--
    fun setPosition(newPos: Position) {
        getPageFromPosition(currentPosition)?.also { newPage ->
            currentPosition = newPos
            currentPageLiveData.value = newPage
        }
    }

    // find position in keyboard
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
        return (pos + length)%length
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

    fun getProjectedPages(original: List<PageData>) : List<PageData> {
        val app = App.getInstance(getApplication<Application>().applicationContext)
        return original
            .let {
                RemoveBlanksProjection("icon").project(it)
            }
            .let {
                LinkedPagesProjection(app.get("createLinks").toString()).project(it)
            }
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