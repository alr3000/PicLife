package com.hyperana.kindleimagekeyboard

import androidx.lifecycle.Lifecycle


//todo: pagelistfactory that handles an update of pagelist?

open class PageListNavigator(
    val pageList: List<PageId> = listOf(),
    val state: ObservableNavigationState
): PageNavigator, ObservableNavigationState by state {

    private var currentIndex = 0
    set(value) {
        val temp = field
        field = value
        if (value != temp)
            state?.updateCurrentPage(getPageAt(value))
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

    override fun getPageAt(index: Int) : PageId? {
        return pageList.getOrNull(index)
    }

    override fun goToIndex(i: Int): PageId? {
        return getPageAt(i)
            ?.also {
                currentIndex = i
            }
    }

    override fun goToId(id: PageId): PageId? {
        return pageList.indexOf(id)
            .let { if (it < 0) null else it }
            ?.also {
                currentIndex = it
            }
    }

    override fun observeCurrentPage(lifecycle: Lifecycle, observer: (PageId?) -> Unit) {

    }
}

// returns new pageId or null if movement not possible
class TwoAxisPageNavigator(throughList: List<PageId>,
                            altList: List<PageId>,
                           state: ObservableNavigationState,
                           val throughIsHorizontal: Boolean = true) : PageListNavigator(throughList.plus(altList), state) {

    val TAG = "TwoAxisPageNavigator"

    val nullState = object: ObservableNavigationState {
        override fun observeCurrentPage(lifecycle: Lifecycle, observer: (PageId?) -> Unit) {}
        override fun updateCurrentPage(id: PageId?) {}
    }


    var throughNav = PageListNavigator(throughList, nullState).apply { looping = true }
    var altNav = PageListNavigator(altList, nullState).apply {
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
            Direction.BACK -> super.nextIndexOrNull(d)
            else -> convertDirection(d)?.let { getNav().nextIndexOrNull(it) }
        }
    }

    override fun peek(d: Direction): PageId? {
        return convertDirection(d)?.let { getNav().peek(it) }
    }

    override fun go(d: Direction): PageId? {
       return when(d) {
           Direction.FORWARD,
           Direction.BACK -> super.go(d)
           else -> convertDirection(d)
               ?.let { getNav().go(it) }
               ?.also {super.goToId(it) }
       }
    }

    // cannot request page by index as this is ambiguous
    override fun goToIndex(i: Int): PageId? { throw Exception("Not Supported") }
    override fun getPageAt(i: Int): PageId? { throw Exception("Not Supported") }

    companion object {
        // put this in the alt list in the position where the throughlist should "show through":
        val NEUTRAL_PLACEHOLDER: PageId = -1344

    }

}