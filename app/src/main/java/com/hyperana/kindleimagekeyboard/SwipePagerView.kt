package com.hyperana.kindleimagekeyboard

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Created by alr on 9/21/17.
 *
 * horizontal linear layout.
 *
 * swiping any direction may change current page view depending on model's navigation logic.
 * This pager only ever knows about the currently selected page. No other pages are visible.
 */
class SwipePagerView : FrameLayout {
    constructor(myContext: Context) : super(myContext)
    constructor(myContext: Context, attributeSet: AttributeSet) : super(myContext, attributeSet)

    val TAG = "SwipePagerView"
    var isSwipeOn = true
    var isTrailsOn = false

    var swiper: OrientedSwipeListener? = null
    var verticalSwiper: OrientedSwipeListener? = null
    var trails: TrailTouchListener? = null

    var aacViewModel: AACViewModel
    var actionManager = (context as? MainActivity)?.actionManager

    // keep this as possible convert view when page changes:
    var currentView: InputPageView? = null


    init {
        loadSettings()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // observe model's current page:
        aacViewModel = ViewModelProvider(context as ViewModelStoreOwner)[AACViewModel::class.java]
        aacViewModel.liveCurrentPage.observe(context as LifecycleOwner) {
            it?.also { setPageView(it, currentView) }
        }

        // navigate on horizontal swipe:
        swiper = object: OrientedSwipeListener(
            false, PreferenceManager.getDefaultSharedPreferences(
                context
            )!!, wm
        ) {
            override fun doSwipe(forward: Boolean) {
                Log.d(TAG, "swipe horizontal: right?$forward")
                if (forward) { aacViewModel.goRight(1) }
                else aacViewModel.goLeft(1)
            }
        }

        // navigate on vertical swipe:
        verticalSwiper = object: OrientedSwipeListener(
            true,
            PreferenceManager.getDefaultSharedPreferences(context)!!,
            wm
        ) {
            override fun doSwipe(forward: Boolean) {
                Log.d(TAG, "swipe vertical: down?$forward")
                if (forward) { aacViewModel.goUp(1) }
                else aacViewModel.goDown(1)
            }
        }

        // show visual feedback:
        trails = TrailTouchListener(PreferenceManager.getDefaultSharedPreferences(context)!!)
        this.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }


    fun loadSettings() {
        val settings = PreferenceManager.getDefaultSharedPreferences(context).all
        Log.d(TAG, "settings: swipe on?" + settings.get("doSwipe"))
        isSwipeOn = settings.get("doSwipe")?.toString()?.toBoolean() ?: true
        isTrailsOn = settings.get("doTrails")?.toString()?.toBoolean() ?: true
    }


    fun setPageView(data: PageData, view: InputPageView?) {
        val convertVH = (view?.tag as? PageViewHolder)
        Log.d(TAG, "setPageView(${data.name}) replacing ${convertVH?.page?.name} with ${data.icons.size} icons")

        convertVH
            ?.apply { page = data }

            ?: InputPageView(context)
                .also {
                    it.tag = PageViewHolder(it).apply { page = data}
                    removeAllViews()
                    addView(it)
                }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        Log.d(TAG, "onWindowVisibilityChanged")
        loadSettings()
    }


    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        Log.v(TAG, "onInterceptTouch: " + ev.toString())

        // trails should get all events, but consume none
        if (isTrailsOn) {
            Log.v(TAG, "sent to trails")
            trails?.onTouch(this, ev)
        }

        // swiper should get all events, but consume none
        if (isSwipeOn) {
            Log.v(TAG, "sent to swiper")
            swiper?.onTouch(this, ev)

            //consume to keep events away from children (icons) once a swipe is happening
            //from now on events come to this.onTouchEvent below
            if (swiper?.isSwiping == true) {
                return true
            }
        }

        // check for vertical swipe last:
        Log.v(TAG, "sent to vert swiper")
        verticalSwiper?.onTouch(this, ev)
        return verticalSwiper?.isSwiping == true


        //return false to let unconsumed events go to children

    }

    // receives events while swipe isSwiping as well as when icons have not consumed it
    // in the second case, must return true to DOWN to continue to see MOVES?!
    // todo: -?- fix if swipe starts, icons never get touches
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.v(TAG, "onTouch: " + event.toString())
        // trails should get all events, but consume none
        if (isTrailsOn) {
            trails?.onTouch(this, event)
        }

        // must return true as long as swipe is interested in it
        swiper?.also { if ( isSwipeOn &&  it.isSwiping) return it.onTouch(this, event) }
        verticalSwiper?.also { if (it.isSwiping) return it.onTouch(this, event) }

        // return true to DOWNs that children (icons) have passed so you get the MOVES that follow
        return (event?.action == MotionEvent.ACTION_DOWN)
    }


}