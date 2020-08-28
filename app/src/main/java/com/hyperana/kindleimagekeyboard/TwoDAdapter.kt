package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView


//positions are Pair<Int, Int>.
// when alt axis position == altRestingPosition, main axis page is shown
// position is (main, alt)
typealias Position = Pair<Int,Int>

// items stacked like negativelist[] - [placeholder] - positivelist[]
class WingsAdapter(val positiveList: List<PageData>, val negativeList: List<PageData>) : PageAdapter(listOf()) {
    override val TAG: String = "WingsAdapter"

    val restPosition = negativeList.size

    override fun getAllItems() : List<PageData>  = negativeList.plus(PageData()).plus(positiveList)

    override fun getCount(): Int {
        return getAllItems().count()
    }

    override fun getItem(position: Int): PageData {
        return when {
            position == restPosition -> null
            position < restPosition -> negativeList.getOrNull(Math.abs(position - restPosition) - 1)
            else -> positiveList.getOrNull((position - restPosition) - 1)
        } ?: PageData()
    }

    override fun getItemId(position: Int): Long {
       return getItem(position).hashCode().toLong()
    }

    override fun setSelection(position: Int?) {
        if (position?.let { it in 0 until count } == false) return

        super.setSelection(position)
    }
}


// combines two pageAdapters for up, down, left, right movement.
// main adapter pages are viewed when the alt is in a rest position "between" two lists.
// member pageadapters report selection to listener for view insertion.
open class TwoDAdapter(mainPages: List<PageData>, upPages: List<PageData>, downPages: List<PageData>) : PageAdapter() {

    override val TAG = "TwoDPageAdapter"

    private var mainAxis: PageAdapter? = PageAdapter(mainPages)
    private var altAxis: WingsAdapter? = WingsAdapter(upPages, downPages)

    private var currentPosition: Position = Pair(0,0)

    private val shouldShowAlt: Boolean
        get() = (currentPosition.second != (altAxis?.restPosition ?: 0))

    override var pageListener: PageSelectionListener? = null
        set(value) {
            field = value
            mainAxis?.pageListener = value
            altAxis?.pageListener = value
        }

    override fun getCount(): Int {
       return getAdapter().getCount()
    }


    //todo: put select logic in super class, override here

    // current effective adapter and selection depends on altAxis position:
    fun getAdapter(): PageAdapter {
        return (if (shouldShowAlt) altAxis else mainAxis) ?: PageAdapter(listOf())
    }
    override fun getSelectedItemPosition(): Int {
        return if (shouldShowAlt) currentPosition.second
        else currentPosition.first
    }



    override fun getSelectedItem(): Any? {
        return getAdapter().getItem(getSelectedItemPosition())
    }

    override fun getSelectedItemId(): Long {
        return getAdapter().getItemId(getSelectedItemPosition()) ?: 0L
    }


    // sets main axis selected position, shows main axis(overriding current alt position):
    override fun setSelection(position: Int?) {
        Log.d(TAG, "setSelection: $position")
        setPosition(Pair(position ?: 0, altAxis?.restPosition ?: 0))
    }

    // sets alt axis position, which shows alt axis item if != restPosition:
    fun setAltSelection(pos: Int) {
        Log.d(TAG, "setAltSelection: $pos")
        setPosition(Pair(currentPosition.first, pos))

    }

    override fun setSelectionByPageId(id: String) {
        altAxis?.getAllItems()?.indexOfFirst { it.id == id }
            ?.also { if (it != -1) setAltSelection(it) }
            ?: mainAxis?.getAllItems()?.indexOfFirst { it.id == id}
                ?.also { if (it != -1) setSelection(it) }
    }

    override fun getSelectedView(parent: ViewGroup, convertibleView: InputPageView?): View {
        return getAdapter().getSelectedView(parent, convertibleView)
    }

    // move alt to rest position so that main axis selection is shown:
    fun goToMainAxis() {
        setAltSelection(altAxis?.restPosition ?: 0)
    }

    // selects next view in alt axis (up is positive):
    fun moveOnAltAxis(num: Int = 1) {
        setAltSelection(currentPosition.second + num)
    }

    // selects next view in main axis IFF alt is at rest:
    fun moveOnMainAxis(num: Int = 1) {
        if (getAdapter() == mainAxis)
            setSelection(currentPosition.first + num)
    }


    // resolve page to be viewed and select it on respective adapter:
    private fun setPosition(pos: Position) {
        Log.d(TAG, "setPosition:$pos")
        Pair(
            pos.first, // main axis is looping -- accepts all indices
            altAxis?.let { pos.second.coerceIn(0 until it.count) } ?: 0
        ).also { newPos ->
            if (newPos.toString() != currentPosition.toString()) {
                currentPosition = newPos
                Log.i(TAG, "currentPosition: $currentPosition")
                getAdapter().setSelection(getSelectedItemPosition())
            }
        }
    }


}