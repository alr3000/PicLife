package com.hyperana.kindleimagekeyboard

import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.MutableLiveData


class IconPageTouchHandler(val liveIconEvent: MutableLiveData<IconEvent?>) {
    val TAG = "IconPageTouchHandler"

    var startTouchView: View? = null

    fun findIconCellByWindowCoordinate(views: List<View>, x: Float, y: Float) : View? {
        return views.filter { it.tag is IconData}.find {
            isInView(x, y, it)
        }
    }


    fun eventToWindowCoords(receiver: View, eventX: Float, eventY: Float) : Pair<Float, Float> {
        val windowCoords = intArrayOf(0,0)
        receiver.getLocationInWindow(windowCoords)
        return Pair(eventX + windowCoords[0], eventY + windowCoords[1])
    }

    // compare all in same (window/root?) coordinates
    fun isInView(x: Float, y: Float, v: View) : Boolean {

        val rect = Rect()
        v.getGlobalVisibleRect(rect)
        return rect.contains(x.toInt(), y.toInt())
        /*  val vX = rect.left
          val vY = rect.top
          Log.v(TAG, "isInView" + "(" + x + "," + y + "): " + rect.toString())
          return (vX < x) && (vY < y) && (vX + v.width > x) && (vY + v.height > y)*/
    }


    fun onTouchEvent(receivingView: View, iconViews: List<View>, event: MotionEvent?, touchAction: String?): Boolean {

        val (windowX, windowY) = eventToWindowCoords(receivingView, event!!.x, event!!.y)
        val iconView = findIconCellByWindowCoordinate(iconViews, windowX, windowY)

            fun touchIconHandler(event: MotionEvent?) : Boolean {
                //executes on down
                if (event?.action == MotionEvent.ACTION_DOWN) {

                    // which icon, if any?
                    Log.v(TAG, "touchIconHandler: " +
                            (iconView?.tag as? IconData)?.text + " -- "  + event )
                    if (iconView != null) {

                        //do all
                        liveIconEvent.postValue(
                            IconEvent(
                                iconView.tag as? IconData,
                                ICON_PREVIEW,
                                iconView
                            )
                        )
                        liveIconEvent.postValue(
                            IconEvent(
                                iconView.tag as? IconData,
                                ICON_EXECUTE,
                                iconView
                            )
                        )

                        return true
                    }
                }
                return false
            }

            fun clickIconHandler(event: MotionEvent?) : Boolean {
                // executes on up if you're still on the original icon
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (iconView != null) {
                            liveIconEvent.postValue(
                                IconEvent(
                                    iconView.tag as? IconData,
                                    ICON_PREVIEW,
                                    iconView
                                )
                            )
                        }
                        startTouchView = iconView
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // if don't know where it started or moved out of starting cell, forget it
                        if ((startTouchView != null) &&
                            (!isInView(windowX, windowY, startTouchView!!))) {
                            startTouchView = null
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        startTouchView = null
                    }
                    MotionEvent.ACTION_UP -> {
                        if ((startTouchView != null) && isInView(windowX, windowY, startTouchView!!)) {
                            liveIconEvent.postValue(
                                IconEvent(
                                    iconView?.tag as? IconData,
                                    ICON_EXECUTE,
                                    iconView
                                )
                            )
                        }
                        startTouchView = null
                    }
                }
                Log.d(TAG, "clickIconHandler -- " +
                        (startTouchView?.tag as? IconData)?.text + ": " + event.toString())

                return (startTouchView != null)
            }

            fun releaseIconHandler(event: MotionEvent?) : Boolean {
                // executes on up for icon cursor is currently in, previews icons as it moves through
                if (event == null) { return false }
                Log.v(TAG, "releaseIconHandler -- " +
                        (iconView?.tag as? IconData)?.text + ": " + event.toString())
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startTouchView = iconView
                        liveIconEvent.postValue(
                            IconEvent(
                                iconView?.tag as? IconData,
                                ICON_PREVIEW,
                                iconView
                            )
                        )
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if ((iconView != null) && (iconView != startTouchView)) {
                            liveIconEvent.postValue(
                                IconEvent(
                                    iconView.tag as? IconData,
                                    ICON_PREVIEW,
                                    iconView
                                )
                            )
                        }
                        startTouchView = iconView
                        return true
                    }
                    // should receive canceled events after swipe has consumed them:
                    // return true to pick up if swipe drops? It doesn't.
                    MotionEvent.ACTION_CANCEL -> {
                        startTouchView = null
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (iconView != null) {
                            if (iconView != startTouchView) {
                                liveIconEvent.postValue(
                                    IconEvent(
                                        iconView.tag as? IconData,
                                        ICON_PREVIEW,
                                        iconView
                                    )
                                )
                            }
                            liveIconEvent.postValue(
                                IconEvent(
                                    iconView.tag as? IconData,
                                    ICON_EXECUTE,
                                    iconView
                                )
                            )
                            startTouchView = null
                        }
                        return false
                    }
                }
                return false
            }





            return when (touchAction) {
                "touchActionDown" -> touchIconHandler(event)
                "touchActionClick" -> clickIconHandler(event)
                "touchActionUp" -> releaseIconHandler(event)
                else -> {
                    Log.w(TAG, "onTouchEvent ignores unknown touchAction")
                    false
                }
            }



    }
}