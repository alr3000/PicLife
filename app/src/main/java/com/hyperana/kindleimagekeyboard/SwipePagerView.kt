package com.hyperana.kindleimagekeyboard

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Adapter
import android.widget.LinearLayout

/**
 * Created by alr on 9/21/17.
 *
 * horizontal linear layout.
 * add child views
 * one child fills parent width and determines pager's height
 * first child is visible by default
 * swiping left or right changes visible child
 */
class SwipePagerView : LinearLayout {
    constructor(myContext: Context) : super(myContext)
    constructor(myContext: Context, attributeSet: AttributeSet) : super(myContext, attributeSet)

    val TAG = "SwipePagerView"
    var currentPageIndex = 0
    var currentPageVerticalIndex = 0
    var isSwipeOn = true
    var isTrailsOn = false
    var isFirstChild = true

    var data: List<PageData> = listOf()
    var adapter: Adapter? = null
    var verticalAdapter: Adapter? = null

    var swiper: OrientedSwipeListener? = null
    var verticalSwiper: OrientedSwipeListener? = null
    var trails: TrailTouchListener? = null
    var pageListener: PageListener? = null
    var verticalPageListener: PageListener? = null

    interface PageListener {
        fun onPageChange(page: PageData?, index: Int)
       // fun getNextPage(direction: Int): PageData?
    }


    init {
        loadSettings()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // set page on swipe:
        swiper = object: OrientedSwipeListener(false, PreferenceManager.getDefaultSharedPreferences(context)!!, wm) {
            override fun doSwipe(forward: Boolean) {
                setPage(getNext(forward))
            }
        }
        // set page on swipe:
        verticalSwiper = object: OrientedSwipeListener( true,
            PreferenceManager.getDefaultSharedPreferences(context)!!,
            wm
        ) {
            override fun doSwipe(forward: Boolean) {
                setVerticalPage(forward)
            }
        }
        trails = TrailTouchListener(PreferenceManager.getDefaultSharedPreferences(context)!!)
        this.orientation = LinearLayout.HORIZONTAL
        this.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, 0)
    }



    override fun addView(child: View?) {
        Log.d(TAG, "addView")
        super.addView(child)

        // set LayoutParams to match parent
        child?.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        )
    }

    fun loadSettings() {
        val settings = PreferenceManager.getDefaultSharedPreferences(context).all
        Log.d(TAG, "settings: swipe on?" + settings.get("doSwipe"))
        isSwipeOn = settings.get("doSwipe")?.toString()?.toBoolean() ?: true
        isTrailsOn = settings.get("doTrails")?.toString()?.toBoolean() ?: true
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        Log.d(TAG, "onWindowVisibilityChanged")
        loadSettings()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.d(TAG, "onLayout:" + listOf(l, t, r, b).joinToString(","))
        super.onLayout(changed, l, t, r, b)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        Log.d(TAG, "onInterceptTouch: " + ev.toString())

        // trails should get all events, but consume none
        if (isTrailsOn) {
            Log.v(TAG, "sent to trails")
            trails?.onTouch(this, ev)
        }

        // swiper should get all events, but consume none
        if (isSwipeOn) {
            Log.d(TAG, "sent to swiper")
            swiper?.onTouch(this, ev)

            //consume to keep events away from children (icons) once a swipe is happening
            //from now on events come to this.onTouchEvent below
            if (swiper?.isSwiping == true) return true
        }

        // check for vertical swipe last:
        Log.d(TAG, "sent to vert swiper")
        verticalSwiper?.onTouch(this, ev)
        return verticalSwiper?.isSwiping == true


        //return false to let unconsumed events go to children

    }

    // receives events while swipe isSwiping as well as when icons have not consumed it
    // in the second case, must return true to DOWN to continue to see MOVES?!
    // todo: -?- fix if swipe starts, icons never get touches
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.d(TAG, "onTouch: " + event.toString())
        // trails should get all events, but consume none
        if (isTrailsOn) {
            trails?.onTouch(this, event)
        }

        // must return true as long as swipe is interested in it
        swiper?.also { if (isSwipeOn && it.isSwiping) return it.onTouch(this, event) }
        verticalSwiper?.also { if (it.isSwiping) return it.onTouch(this, event) }

        // return true to DOWNs that children (icons) have passed so you get the MOVES that follow
        return (event?.action == MotionEvent.ACTION_DOWN)
    }

    //todo: real two-D adapter 
    //*************************** HANDLE PAGES *******************************************
    fun setVerticalPage(forward: Boolean) {
        Log.i(TAG, "next vertical page, " + if(forward) "UP" else "DOWN")
        setPage(getNextVertical(forward))
    }

    fun getNextVertical(forward: Boolean) : Int {
         return verticalAdapter?.count?.let {
             (currentPageVerticalIndex + (if (forward) 1 else -1)).coerceIn(0 until it)
         } ?: currentPageVerticalIndex
    }

    fun getNext(forward: Boolean) : Int{
        val total = adapter!!.count
        return (total + currentPageIndex + (if (forward) 1 else -1)) % total
    }

    // swipe pager simply replaces the current page with the new one
    // since the views are all the same, current view is offered as convertView
    fun setPage(position: Int) {
        Log.i(TAG, "set page: $position")
        try {

            // Check whether we have a transient state view. Attempt to re-bind the
            // data and discard the view if we fail. After ListView source code.
            if (childCount > 0) {
                val convertibleView = getChildAt(0)
                val updatedView = adapter?.getView(position, convertibleView, this);

                // If we failed to re-use the convertible view, remove it
                if (updatedView != convertibleView) {
                    removeView(convertibleView)
                    addView(updatedView)
                }
            }
            else {
                addView(adapter?.getView(position, null, this))
            }
            currentPageIndex = position

            pageListener?.onPageChange(adapter?.getItem(position) as? PageData, position)
        }
        catch(e: Exception) {
            Log.w(TAG, "couldn't set page " + position, e)
        }
    }

    fun setPageById(id: String) {
        try {
            (0..adapter!!.count - 1).first {
                (adapter!!.getItem(it) as? PageData)?.id == id
            }.also {
                setPage(it)
            }
        }
        catch (e: Exception) {
            Log.w(TAG, "coudn't find requested page: " + id, e)
            //do nothing
        }
    }
}