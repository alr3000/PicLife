package com.hyperana.kindleimagekeyboard

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*

typealias PageId = Int
enum class Direction {
    LEFT, RIGHT, UP, DOWN, FORWARD, BACK
}

interface PageNavigator  {
    fun nextIndexOrNull(d: Direction): Int?
    fun go(d: Direction): PageId?
    fun peek(d: Direction): PageId?
    fun goToIndex(i: Int): PageId?
    fun goToId(id: PageId): PageId?
    fun getPageAt(i: Int): PageId?
    fun getCurrentIndex(): Int
    fun getCurrentPage(): PageId?
}

interface PageHistory {
    fun addPage(pageId: PageId)
    fun getCurrentPage(): PageId?
    fun getPreviousPage(): PageId?
    fun getNextPage(): PageId?
    fun gotoNext(): PageId?
    fun gotoPrevious(): PageId?
}

interface ObservableNavigationState {
    fun observeReady(lifecycle: Lifecycle, observer:(Boolean) -> Unit)
    fun observeCurrentPage(lifecycle: Lifecycle, observer: (PageId?) -> Unit)
    fun updateCurrentPage(id: PageId?)
    fun updateReady(isReady: Boolean)
}

class LiveDataNavigationState : ObservableNavigationState {
    private val currentPageLiveData = MutableLiveData<PageId?>()
    private val readyLiveData = MutableLiveData<Boolean>()
    val TAG = "LiveDataNavigationState"

    override fun observeCurrentPage(lifecycle: Lifecycle, observer: (PageId?) -> Unit) {
        currentPageLiveData.observe({lifecycle}, observer)
    }

    override fun observeReady(lifecycle: Lifecycle, observer: (Boolean) -> Unit) {
        readyLiveData.observe({lifecycle}, observer)
    }

    override fun updateReady(isReady: Boolean) {
        readyLiveData.postValue(isReady)
    }

    override fun updateCurrentPage(id: PageId?) {
        Log.d(TAG, "updateCurrentPage: $id")
        currentPageLiveData.postValue(id)
    }
}

// This class Android-izes the AACModel and sets up livedata observation for views, informed by
// SharedPreferences and RoomDatabase Respository

// This model holds the keyboard, aac pages, recents pages, and tools pages AS IDs,
// it handles the logic of the AAC page/keyboard navigation and "exports" the page
// currently being requested/viewed:
// todo: repository built with cache
class AACViewModel private constructor (application: Application,
                                        val state: ObservableNavigationState,
                                        val repository: AACRepository
                                        )
    : AndroidViewModel(application),
    SharedPreferences.OnSharedPreferenceChangeListener,
    ObservableNavigationState by state,
        ResourceInflater by repository
{

    // single-param constructor for viewmodelprovider:
    constructor(application: Application) :
            this(application, LiveDataNavigationState(), AACRepository(AppDatabase.getDatabase(application.applicationContext)!!))

    val TAG = "AACViewModel${hashCode()}"

    // saved data:
    val EXTRA_KEYBOARD_ID = "keyboard_id"
    val EXTRA_PAGE_ID = "page_id"

    val RECENTS_PAGE_ID = 1003
    val TOOLS_PAGE_ID = 1004

    // structural vars for creating AACModel and Navigation:
    val app = App.getInstance(application.applicationContext)
    val context = application.applicationContext

    // state vars:
    var model: AACModel? = null
    var modelJob: Job? = null




/*
// todo handled by recents page back and forward button?

    val history: PageHistory = object : PageHistory {

        var list: List<PageId> = listOf()
        var index = -1

        override fun addPage(pageId: com.hyperana.kindleimagekeyboard.PageId) {
            list = list.take(index + 1).plus(pageId)
            index++
        }

        override fun getCurrentPage(): com.hyperana.kindleimagekeyboard.PageId? {
            return list.getOrNull(index)
        }

        override fun getPreviousPage(): com.hyperana.kindleimagekeyboard.PageId? {
            return list.getOrNull(index - 1)
        }

        override fun getNextPage(): com.hyperana.kindleimagekeyboard.PageId? {
            return list.getOrNull(index + 1)
        }

        override fun gotoNext(): com.hyperana.kindleimagekeyboard.PageId? {
            return getNextPage()?.also { index++ }
        }

        override fun gotoPrevious(): com.hyperana.kindleimagekeyboard.PageId? {
            return getPreviousPage()?.also { index-- }
        }
    }

*/

    init {
        PreferenceManager.getDefaultSharedPreferences(application.applicationContext)
            .also { prefs ->
                prefs.registerOnSharedPreferenceChangeListener(this)
              //  onRestoreInstanceState(null, prefs)
            }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.i(TAG, "onPreferenceChanged: $key")

        key ?: return

        if (shouldRefreshKeyboardOnChange(key)) {
            onRestoreInstanceState(null, sharedPreferences!!)
        }

    }



    // set keyboard and current page from saved or preferences if out-of-whack
    fun onRestoreInstanceState(savedInstanceState: Bundle?, prefs: SharedPreferences) {
        Log.d(TAG, "restore instance state: $savedInstanceState")
        val NOT_AVAIL = -1

        var kid = savedInstanceState
            ?.getInt(EXTRA_KEYBOARD_ID, NOT_AVAIL)
            ?.let { if (it == NOT_AVAIL) null else it }
            ?.also { Log.d(TAG, "saved id: $it") }
            ?: prefs.getInt("currentKeyboardId", DEFAULT_KEYBOARD_ID)

        // assure current keyboard is correct, update model if nec.:
        if (kid == model?.keyboard?.id) return

        // cancel any currently active job:
            if (modelJob?.isActive == true) {
                Log.i(TAG, "cancelling active modelJob...")
                modelJob?.cancel()
            }
        modelJob = loadModelData()
        savedInstanceState?.getInt(EXTRA_PAGE_ID)
            ?.also { pageId ->
                modelJob!!.invokeOnCompletion { model?.goToId(pageId)}
            }

    }



    // store current keyboard id, page id:
    fun onSaveInstanceState(outState: Bundle?) : Bundle {
        return (outState ?: Bundle()).also { saved ->
            (model?.keyboard?.id ?: -1).also { saved.putInt(EXTRA_KEYBOARD_ID, it)  }
            (model?.getCurrentPage() ?: -1)
                .also { saved.putInt(EXTRA_PAGE_ID, it)}
        }
    }

    fun loadModelData() : Job {
        return CoroutineScope(Dispatchers.IO).launch {
            state.updateReady(false)

            Log.i(TAG, "loadModelData")
            model = AACModel.Builder().apply {
                this.repository = this@AACViewModel.repository
                keyboardId =
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .getInt(PREF_KEYBOARD_ID, DEFAULT_KEYBOARD_ID)
                upList = listOf<PageId>(RECENTS_PAGE_ID)
                downList = listOf(TOOLS_PAGE_ID)
                projection = { list -> getProjectedPages(list) }
                observableState = state

            }.create()

            state.updateReady(true)

        }

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