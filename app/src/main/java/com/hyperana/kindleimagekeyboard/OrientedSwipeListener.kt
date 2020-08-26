package com.hyperana.kindleimagekeyboard

import android.content.SharedPreferences
import android.graphics.Point
import android.util.Log
import android.view.*

/**
 * Created by alr on 8/23/17.
 *
 *
 */
// swipe is defined as a single touch that moves left or right within a narrow band
// swiper returns true to consume the event from the beginning, but isswiping remains false for the
// first bit so an interceptor can be alerted when swiper should supercede other consumers
abstract class OrientedSwipeListener(val isVertical: Boolean, val settings: SharedPreferences, windowManager: WindowManager) : View.OnTouchListener {
    val TAG = "VerticalSwipeListener"

    // SETTINGS:
    var BAND_WIDTH = 100
    var MIN_LENGTH = 300
    var MAX_TIME =  500L
    var dimen = Point()

    // swipe controls:
    var startTime: Long? = null
    var startMain: Float? = null // starting position on swipe axis
    var isSwiping: Boolean = false

    var auxMin: Float = 0.toFloat() // range allowed on perpendicular axis
    var auxMax: Float = 0.toFloat()

    init {
        windowManager.defaultDisplay.getRealSize(dimen)
        loadSettings()
    }

    // execute swipe:
    abstract fun doSwipe(forward: Boolean)

    fun loadSettings() {

        val type = settings.all.get("swipeType")?.toString()
        val screenSize = if (isVertical) dimen.y else dimen.x
        Log.i(TAG, "loadSettings: $type ($screenSize)")
        when (type) {
            "easy" -> {
                BAND_WIDTH = screenSize/4
                MIN_LENGTH = screenSize/2
                MAX_TIME = 1000L
            }
            "medium" -> {
                BAND_WIDTH = screenSize/6
                MIN_LENGTH = (screenSize * 0.7).toInt()
                MAX_TIME = 500L
            }
            "hard" -> {
                BAND_WIDTH = screenSize/6
                MIN_LENGTH = (screenSize * 0.7).toInt()
                MAX_TIME = 200L
            }
        }
    }

    // compare new point to allowed range set by starting position:
    fun isInBand(newX: Float, newY: Float) : Boolean {
        return if (isVertical) auxMin < newX && auxMax > newX
            else auxMin < newY && auxMax > newY
    }

    fun isInTime() : Boolean {
        return (startTime != null) && (System.currentTimeMillis() < startTime!! + MAX_TIME)
    }

    fun startSwipe(x: Float, y: Float) {
        Log.d(TAG, "start: $x,$y")
        startTime = System.currentTimeMillis()
        startMain = if (isVertical) y else x
        isSwiping = false
        auxMin = if (isVertical) x - BAND_WIDTH else y - BAND_WIDTH
        auxMax = if (isVertical) x + BAND_WIDTH else y + BAND_WIDTH
    }

    fun isHalfway(x: Float, y: Float) : Boolean {
            return getDistance(x, y)?.let { it > MIN_LENGTH / 2 } == true
    }

    fun isForward(x: Float, y: Float, start: Float) : Boolean {
        return if (isVertical) y > start else x > start
    }

    fun clearSwipe() {
        startMain = null
        startTime = null
        isSwiping = false
        loadSettings() // check for changes when not expected to do anything
    }

    fun getDistance(x: Float, y: Float) : Float? {
        return startMain?.let {
            Math.abs(if(isVertical) y - it else x - it)
        }.also { Log.d(TAG, "distance: $it")}
    }

    fun isComplete(x: Float, y: Float): Boolean {
        return  (isInTime() && isInBand(x, y) && (getDistance(x, y)?.let { it > MIN_LENGTH } == true))
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        try {
            when (event?.action) {
                KeyEvent.ACTION_DOWN -> {
                    Log.d(TAG, "DOWN: ${event.x},${event.y}")
                    startSwipe(event.x, event.y)
                }
                KeyEvent.ACTION_UP -> {
                    Log.d(TAG, "UP: ${event.x},${event.y}")
                    startMain?.also { start ->
                        if (isComplete(event.x, event.y)) doSwipe(isForward(event.x, event.y, start))
                    }
                    clearSwipe()

                }
                MotionEvent.ACTION_CANCEL -> {
                    Log.d(TAG, "CANCEL")
                    clearSwipe()
                }
                MotionEvent.ACTION_MOVE -> {
                    Log.d(TAG, "MOVE: ${event.x},${event.y}")
                    startMain?.also {start ->
                        if (!isInBand(event.x, event.y) || !isInTime()) {
                            Log.d(TAG, "swipe out of bounds")
                            clearSwipe()
                        }
                        else {
                            isSwiping = isHalfway(event.x, event.y)
                            Log.d(TAG, "onTouch: check isSwiping? " + isSwiping.toString())
                        }
                    }

                }
            }
            return true // must return true to receive MOVE and UP events!

        }
        catch (e: Exception) {
            Log.e(TAG, "failed swipe motion", e)
            return false
        }
    }
}
