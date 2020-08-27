package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.ListView


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
            position < restPosition -> negativeList.getOrNull((position - restPosition) - 1)
            else -> positiveList.getOrNull((position - restPosition) - 1)
        } ?: PageData()
    }

    override fun getItemId(position: Int): Long {
       return getItem(position).hashCode().toLong()
    }

}


// change pages by selection:
open class TwoDAdapterView : AdapterView<PageAdapter> {

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) :
            super(context, attributeSet, defStyleAttr)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attributeSet, defStyleAttr, defStyleRes)


    private val TAG = "TwoDPageAdapter"

    private var mainAxis: PageAdapter? = null
    private var altAxis: WingsAdapter? = null

    private var currentPosition: Position = Pair(0,0)
    private var currentView: View? = null

    private val shouldShowAlt: Boolean
        get() = (currentPosition.second != (altAxis?.restPosition ?: 0))

    var pageListener: PageListener? = null

    interface PageListener {
        fun onPageChange(page: PageData?, index: Int)
        // fun getNextPage(direction: Int): PageData?
    }


    override fun setAdapter(pAdapter: PageAdapter?) {
        Log.i(TAG, "setAdapter with ${pAdapter?.count} pages")
        mainAxis = pAdapter
    }

    fun setAltAdapter(pAdapter: WingsAdapter?) {
        Log.i(TAG, "setAltAdapter with ${pAdapter?.count} pages")
        altAxis = pAdapter
    }

    // current effective adapter and selection depends on altAxis position:
    override fun getAdapter(): PageAdapter? {
        return if (shouldShowAlt) altAxis else mainAxis
    }
    override fun getSelectedItemPosition(): Int {
        return if (shouldShowAlt) currentPosition.second
        else currentPosition.first
    }



    override fun getSelectedView(): View? {
        return getAdapter()?.getView(getSelectedItemPosition(), currentView, this)
    }

    override fun getSelectedItem(): Any? {
        return getAdapter()?.getItem(getSelectedItemPosition())
    }

    override fun getSelectedItemId(): Long {
        return getAdapter()?.getItemId(getSelectedItemPosition()) ?: 0L
    }


    // sets main axis selected position, shows main axis(overriding current alt position):
    override fun setSelection(position: Int) {
        Log.d(TAG, "setSelection: $position")
        setPosition(Pair(position, altAxis?.restPosition ?: 0))
        onItemSelectedListener?.onItemSelected(
            this, getSelectedView(), getSelectedItemPosition(), getSelectedItemId()
        )
    }

    // sets alt axis position, which shows alt axis if != restPosition:
    fun setAltSelection(pos: Int) {
        Log.d(TAG, "setAltSelection: $pos")
        setPosition(Pair(currentPosition.first, pos))
        onItemSelectedListener?.onItemSelected(
            this, getSelectedView(), getSelectedItemPosition(), getSelectedItemId()
        )
    }

    fun setSelectionByPageId(id: String) {
        altAxis?.getAllItems()?.indexOfFirst { it.id == id }
            ?.also { if (it != -1) setAltSelection(it) }
            ?: mainAxis?.getAllItems()?.indexOfFirst { it.id == id}
                ?.also { if (it != -1) setSelection(it) }
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


    private fun setPosition(pos: Position) {
        Log.d(TAG, "setPosition:$pos")
        currentPosition = Pair(
            pos.first.coerceIn(0 until (mainAxis?.count ?: 1)),
            pos.second.coerceIn(0 until (altAxis?.count ?: 1))
        )
        Log.d(TAG, "currentPosition: $currentPosition")

        setChildView()
        pageListener?.onPageChange(selectedItem as? PageData, currentPosition.second)

    }


    // implement ViewGroup abstract methods:
    val defaultView = LinearLayout(context).apply { layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)}

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        Log.d(TAG, "onlayout: $currentView")
        /*if (childCount == 0)
            addViewInLayout(currentView ?: defaultView, 0, LayoutParams(MATCH_PARENT, MATCH_PARENT))*/
        currentView?.layout(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas?) {
        Log.d(TAG, "ondraw: $currentView")
        super.onDraw(canvas)

        currentView?.draw(canvas)
    }

    override fun addView(child: View?) {
        Log.d(TAG, "addView")
        (child ?: defaultView).also { addViewInLayout(it, 0, it.layoutParams) }
    }

    // only ever one view:
    override fun removeView(child: View?) { removeAllViews() }
    override fun removeViewAt(index: Int) { removeAllViews() }
    override fun removeAllViews() {
        removeAllViewsInLayout()
    }

    private fun setChildView() {

        try {

            // Check whether we have a transient state view. Attempt to re-bind the
            // data and discard the view if we fail. After ListView source code.
            val updatedView = getSelectedView()

            // If we failed to re-use the convertible view, remove it
            if (updatedView != currentView) {
                removeAllViews()
                currentView = updatedView
                addView(currentView ?: defaultView)

                // overriden methods don't call requestlayout:
                requestLayout()
            }

        }
        catch(e: Exception) {
            Log.w(TAG, "couldn't set page " + currentView, e)
        }
    }
}