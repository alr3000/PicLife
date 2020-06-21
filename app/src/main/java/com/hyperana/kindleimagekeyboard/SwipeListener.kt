package com.hyperana.kindleimagekeyboard

import android.content.SharedPreferences
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * Created by alr on 8/23/17.
 *
 *
 */
// swipe is defined as a single touch that moves left or right within a narrow band
// swiper returns true to consume the event from the beginning, but isswiping remains false for the
// first bit so an interceptor can be alerted when swiper should supercede other consumers
abstract class SwipeListener(val settings: SharedPreferences) : View.OnTouchListener {
    val TAG = "SwipeListener"

    // SETTINGS:
    var BAND_WIDTH = 100
    var MIN_LENGTH = 300
    var MAX_TIME =  500L

    // swipe controls:
    var startTime: Long? = null
    var startX: Float? = null
    var isSwiping: Boolean = false

    var yMin: Float = 0.toFloat()
    var yMax: Float = 0.toFloat()

    init {
        loadSettings()
    }

    // execute swipe:
    abstract fun doSwipe(forward: Boolean)

    fun loadSettings() {
        val type = settings.all.get("swipeType")?.toString()
        Log.d(TAG, "loadSettings: " + type)
        when (type) {
            "easy" -> {
                BAND_WIDTH = 200
                MIN_LENGTH = 200
                MAX_TIME = 1000L
            }
            "medium" -> {
                BAND_WIDTH = 100
                MIN_LENGTH = 300
                MAX_TIME = 500L
            }
            "hard" -> {
                BAND_WIDTH = 20
                MIN_LENGTH = 300
                MAX_TIME = 200L
            }
        }
    }

    fun isInBand(newY: Float) : Boolean {
        return (yMin < newY) && (yMax > newY)
    }

    fun isInTime() : Boolean {
        return (startTime != null) && (System.currentTimeMillis() < startTime!! + MAX_TIME)
    }

    fun startSwipe(x: Float, y: Float) {
        startTime = System.currentTimeMillis()
        startX = x
        isSwiping = false
        yMin = y - BAND_WIDTH
        yMax = y + BAND_WIDTH
    }

    fun isHalfway(x: Float) : Boolean {
        return (startX != null) && (Math.abs(x - startX!!) > MIN_LENGTH/2)
    }

    fun clearSwipe() {
        startX = null
        startTime = null
        isSwiping = false
        loadSettings() // check for changes when not expected to do anything
    }

    fun endSwipe(x: Float, y: Float) {
        if (isInTime() && isInBand(y) && (Math.abs(x - startX!!) > MIN_LENGTH)) {
            doSwipe(forward = x > startX!!)
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        try {
            when (event?.action) {
                KeyEvent.ACTION_DOWN -> {
                    Log.d(TAG, "DOWN: " + event.x + "," + event.y)
                    startSwipe(event.x, event.y)
                }
                KeyEvent.ACTION_UP -> {
                    Log.d(TAG, "UP")
                    if (startX != null) {
                        endSwipe(event.x, event.y)
                        clearSwipe()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    Log.d(TAG, "CANCEL")
                    clearSwipe()
                }
                MotionEvent.ACTION_MOVE -> {
                    Log.d(TAG, "MOVE: " + event.y)
                    if (startX != null) {
                        if (!isInBand(event.y) || !isInTime()) {
                            Log.d(TAG, "swipe out of bounds")
                            clearSwipe()
                        }
                        else {
                            isSwiping = isHalfway(event.x)
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
