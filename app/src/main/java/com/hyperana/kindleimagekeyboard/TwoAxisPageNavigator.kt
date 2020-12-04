package com.hyperana.kindleimagekeyboard

import android.util.Log
import androidx.lifecycle.Lifecycle


//todo: pagelistfactory that handles an update of pagelist?

open class PageListNavigator(
    val pageList: List<PageId> = listOf(),
    val state: ObservableNavigationState
): PageNavigator, ObservableNavigationState by state {
    private val TAG = "PageListNavigator"

    private var currentIndex = 0
    set(value) {
        field = value
        state.updateCurrentPage(getPageAt(value))
    }

    var looping = false

    private val currentId: PageId?
    get() { return getPageAt(currentIndex)}



    override fun getCurrentIndex(): Int {
        return currentIndex
    }

    override fun getCurrentPage(): PageId? {
        return currentId
    }

    override fun nextIndexOrNull(d: Direction): Int? {
        return when (d) {
            Direction.FORWARD -> (currentIndex + 1).let {
                if (it < pageList.size) it else if (looping) 0 else null
            }
            Direction.BACK -> (currentIndex - 1).let {
                if (it >= 0) it else if (looping) pageList.size - 1 else null
            }
            else -> null
        }
    }

    override fun peek(d: Direction): PageId? {
        return nextIndexOrNull(d)
            ?.let { getPageAt(it)  }
    }

    override fun go(d: Direction): PageId? {
        return nextIndexOrNull(d)
            ?.also { currentIndex = it }
            ?.let { getPageAt(it) }
    }

    override fun getPageAt(i: Int) : PageId? {
        return pageList.getOrNull(i)
    }

    override fun goToIndex(i: Int): PageId? {
        return getPageAt(i)
            ?.also {
                currentIndex = i
            }
    }

    override fun goToId(id: PageId): PageId? {
        Log.d(TAG, "gotoId: $id")
        return pageList.indexOf(id)
            .let { if (it < 0) null else it }
            ?.also {
                currentIndex = it
            }
    }
}

// returns new pageId or null if movement not possible
class TwoAxisPageNavigator(throughList: List<PageId>,
                            altList: List<PageId>,
                           val state: ObservableNavigationState,
                           val throughIsHorizontal: Boolean = true) : PageNavigator {

    private val TAG = "TwoAxisPageNavigator"

    // don't let "sub-navigators" post any state:
    object NullState : ObservableNavigationState {
        override fun observeCurrentPage(lifecycle: Lifecycle, observer: (PageId?) -> Unit) {
            throw Exception("don't observe secondary navigator")
        }
        override fun updateCurrentPage(id: PageId?) {
           Log.w("NullState", "updating NullState")
        }
        override fun observeReady(lifecycle: Lifecycle, observer: (Boolean) -> Unit) {
            throw Exception("don't observe secondary navigator")
        }
        override fun updateReady(isReady: Boolean) {
            Log.w("NullState", "updating NullState")
        }
    }


    var throughNav = PageListNavigator(throughList, NullState).apply { looping = true }
    var altNav = PageListNavigator(altList, NullState).apply {
        looping = false
        goToId(NEUTRAL_PLACEHOLDER)
    }

    private fun convertDirection(d: Direction) : Direction? {
        return when (d) {
            Direction.LEFT -> if (throughIsHorizontal) Direction.FORWARD else null
            Direction.RIGHT -> if (throughIsHorizontal) Direction.BACK else null
            Direction.UP -> if (!throughIsHorizontal) Direction.BACK else null
            Direction.DOWN -> if (!throughIsHorizontal) Direction.FORWARD else null
            Direction.FORWARD,
            Direction.BACK -> d
        }
    }

    private fun getNav() : PageNavigator {
        return if (altNav.getCurrentPage() == NEUTRAL_PLACEHOLDER) throughNav else altNav
    }

    override fun getCurrentIndex(): Int {
        throw Exception("Not Supported")
    }

    override fun nextIndexOrNull(d: Direction): Int? {
        return when(d) {
            Direction.FORWARD,
            Direction.BACK -> null
            else -> convertDirection(d)?.let { getNav().nextIndexOrNull(it) }
        }
    }

    override fun peek(d: Direction): PageId? {
        return convertDirection(d)?.let { getNav().peek(it) }
    }

    // must call super.goToId() to post changes to observers:
    override fun go(d: Direction): PageId? {
       return when(d) {
           Direction.FORWARD,
           Direction.BACK -> null
           else -> convertDirection(d)
               ?.let { getNav().go(it) }
               ?.also { goToId(it) }
       }
    }

    // Indexed requests assumed to refer to aac (throughNav):
    // must post nav changes to observers here:
    override fun goToIndex(i: Int): PageId? {
        return throughNav.goToIndex(i)

                // no nav if page is null!
            ?.also {  state.updateCurrentPage(it) }
    }
    override fun getPageAt(i: Int): PageId? { return throughNav.getPageAt(i) }

    override fun goToId(id: PageId): PageId? {
        throughNav.goToId(id) ?: altNav.goToId(id)
        return getCurrentPage()
            .also { state.updateCurrentPage(id)}
    }

    override fun getCurrentPage(): PageId? {
        return altNav.getCurrentPage()
            .let { if (it == NEUTRAL_PLACEHOLDER) throughNav.getCurrentPage() else it }
    }

    companion object {
        // put this in the alt list in the position where the throughlist should "show through":
        const val NEUTRAL_PLACEHOLDER: PageId = -1344

    }

}