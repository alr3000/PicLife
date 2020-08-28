package com.hyperana.kindleimagekeyboard

import android.animation.Animator
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * Created by alr on 9/22/17.
 *
 *
 *
 */
class TrailTouchListener(val settings: SharedPreferences) : View.OnTouchListener {
    val TAG = "TrailTouchListener"
    val size = 20
    val fadeTime = 500L
    var hue = 0f

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
             MotionEvent.ACTION_MOVE -> {
             //    Log.d(TAG, "onTouch MOVE")

                 //add to rootview to get correct positioning and z-index
                 val parent = (v?.rootView as? ViewGroup)
                 if (parent != null) {
                     val windowCoords = intArrayOf(-1,-1)
                     v?.getLocationInWindow(windowCoords)

                 //    Log.d(TAG, "add " + (event.historySize + 1) + " blobs")
                     (0 .. event.historySize - 1)
                             .map { Pair(event.getHistoricalX(it), event.getHistoricalY(it))}
                             .toMutableList().plus(Pair(event.x, event.y))
                             .onEach {
                            // .first{
                                 val t = TrailBlob(v!!, event.x + windowCoords[0], event.y + windowCoords[1])
                                 parent.addView(t)
                                 Handler().postDelayed(object : Runnable {
                                     override fun run() {
                                         parent.removeView(t)
                                     }
                                 }, fadeTime)
                             }

                 }
             }
        }
        return true// must return true to receive MOVE and UP events!
    }

    fun getColor() : Int {
        val OPACITY = 125
        val SATURATION = 1f
        val VALUE = 0.5f
        hue = (hue+2f)%360
        return Color.HSVToColor(OPACITY, floatArrayOf(hue, SATURATION, VALUE))
    }

    inner class TrailBlob(view: View, x: Float, y: Float): View(view.context) {

        init {
            layoutParams = ViewGroup.LayoutParams(size, size)
            this.x = x
            this.y = y
            this.bringToFront()
            setBackgroundColor(getColor())
            val animator = animate()
            animator.duration = fadeTime
            animator.alpha(0f)
            animator.scaleY(0f)
            animator.scaleX(0f)
            animator.start()
        }


    }
}