package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import java.io.File
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.*
import java.util.*
import java.util.regex.Pattern


/**
 * Created by alr on 9/12/17.
 *
 * Creates a keyboard style view of the icons (full width and height-adjusted for square icons)
 *
 * Created according to properties in page dataset:
 *  icon indices must be convertible to integer and < grid cell count
 *  page may also specify "color", "rows", "cols", "margins"
 *
 * Uses icon metadata: indexAdjusted must be set
 *
 *
 */

class InputPageView(
        context: Context,
        var page: PageData,
        val color: Int,
        val touchAction: String,
        val iconListener: IconListener) : LinearLayout(context){
    val TAG = "InputPageView - " + page.id

    var views: List<ViewGroup> = listOf()
    var items: List<IconData> = page.icons

    var margins = 10 // percent of cellwidth
    var rows: Int = 3
    var cols: Int = 5

    interface IconListener {
        fun execute(icon: IconData?, v:View?)
        fun preview(icon: IconData?, v:View?)
    }

    // touch listener
    var startTouchView: View? = null



    init {

        // prepare page styles todo: cascading from app, then page overrides
        margins = page.get("margins")?.toIntOrNull() ?: margins
         cols = page.get("cols")!!.toInt()
        rows = page.get("rows")!!.toInt()

        // set page features
        tag = page.name
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(color)

        // add table rows and cells
        views = createGrid(
                table = this,
                rows = rows,
                cols = cols
        )

        // add icon views
        setItemsInViews()
    }



    // set margins after size is known, and before layout so they take effect
    // shouldn't have to be done on refit - todo
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val marr = getMargins(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec))
        views.onEach {
            (it.layoutParams as? LinearLayout.LayoutParams)
                    ?.setMargins(marr[0], marr[1], marr[2], marr[3])
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
     }

    fun refit(newPage: PageData): View? {
        Log.d(TAG, "refit for " + newPage.name)
        try {
            page = newPage
            items = newPage.icons

            views.onEach {
               it.removeAllViews()
                it.tag = null
            }
            setItemsInViews()
            return this
        }
        catch (e: Exception) {
            Log.w(TAG, "could not refit view to " + newPage.name, e)
            return null

        }
    }

    // reuse views if possible

    fun setIconInCell(icon: IconData, cell: ViewGroup) {
        cell.addView(IconData.createView(icon, context, true))
        cell.tag = icon
    }

    fun setItemsInViews() {
        Log.d(TAG, "setItemsInViews")


        items.filter { it.index?.toIntOrNull() != null}.onEach {
            try {
                // icon indices must be convertible to integer and < grid cell count
               setIconInCell(it, views[it.get("indexAdjusted")!!.toInt()])
            }
            catch (e: Exception) {
                Log.w(TAG, "failed set icon " + it.text + " with index " + it.index)
            }
        }.also {
            Log.d(TAG, "set " + it.count() + " items")
        }
    }

    //************************************* TOUCH HANDLERS ***************************************
    // settings: touchAction (touchActionDown, touchActionUp, touchActionClick)

    fun findIconCellByCoordinate(x: Float, y: Float) : View? {
        //logViewCoordinates(this)
        val windowCoords = intArrayOf(0,0)
        getLocationInWindow(windowCoords)

        return views.filter { it.tag is IconData}.find {
            isInView(x + windowCoords[0], y + windowCoords[1], it)
        }
    }

    fun isInIconCell(x: Float, y: Float, v: View) : Boolean {
        val windowCoords = intArrayOf(0,0)
        getLocationInWindow(windowCoords)

        return isInView(x + windowCoords[0], y + windowCoords[1], v)
    }

    // coords relative to rootView
    fun isInView(x: Float, y: Float, v: View) : Boolean {

        val rect = Rect()
        v.getGlobalVisibleRect(rect)
        val vX = rect.left
        val vY = rect.top
        Log.v(TAG, "isInView" + "(" + x + "," + y + "): " + rect.toString())
        return (vX < x) && (vY < y) && (vX + v.width > x) && (vY + v.height > y)
    }

    // depending on selected icon activation type,
    // returns whether event is of interested in future events
    // todo: -L- touch handler is separately defined, added on by IME
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        try {
            return when (touchAction) {
                "touchActionDown" -> touchIconHandler(event)
                "touchActionClick" -> clickIconHandler(event)
                "touchActionUp" -> releaseIconHandler(event)
                else -> {
                    Log.w(TAG, "onTouchEvent ignores unknown touchAction")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "problem with icon touch handler: ", e)
            return false
        }
    }

    fun touchIconHandler(event: MotionEvent?) : Boolean {
        //executes on down
        if (event?.action == MotionEvent.ACTION_DOWN) {

             // which icon, if any?
             val cell = findIconCellByCoordinate(event.x, event.y)
            Log.v(TAG, "touchIconHandler: " +
                    views.indexOf(cell).toString() +
                    (cell?.tag as? IconData)?.text + " -- "  + event )
            if (cell != null) {

                //do all
                iconListener.preview(cell.tag as? IconData, cell)
                iconListener.execute(cell.tag as? IconData, cell)

                return true
            }
        }
        return false
    }

    fun clickIconHandler(event: MotionEvent?) : Boolean {
        // executes on up if you're still on the original icon
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val cell = findIconCellByCoordinate(event.x, event.y)
                if (cell != null) {
                    iconListener.preview(cell.tag as? IconData, cell)
                }
                startTouchView = cell
            }
            MotionEvent.ACTION_MOVE -> {
                // if don't know where it started or moved out of starting cell, forget it
                if ((startTouchView != null) &&
                        (!isInIconCell(event.x, event.y, startTouchView!!))) {
                    startTouchView = null
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                startTouchView = null
            }
            MotionEvent.ACTION_UP -> {
                if ((startTouchView != null) && isInIconCell(event.x, event.y, startTouchView!!)) {
                    iconListener.execute(startTouchView?.tag as? IconData,  startTouchView)
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
        val cell = findIconCellByCoordinate(event.x, event.y)
        Log.d(TAG, "releaseIconHandler -- " +
                views.indexOf(cell).toString() +
                (cell?.tag as? IconData)?.text + ": " + event.toString())
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startTouchView = cell
                iconListener.preview(cell?.tag as? IconData, cell)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if ((cell != null) && (cell != startTouchView)) {
                    iconListener.preview(cell.tag as? IconData, cell)
                }
                startTouchView = cell
                return true
            }
        // should receive canceled events after swipe has consumed them:
        // return true to pick up if swipe drops? It doesn't.
            MotionEvent.ACTION_CANCEL -> {
                startTouchView = null
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (cell != null) {
                    if (cell != startTouchView) {
                        iconListener.preview(cell.tag as? IconData, cell)
                    }
                    iconListener.execute(cell.tag as? IconData, cell)
                    startTouchView = null
                }
                return false
            }
        }
        return false
    }


    //*************************************** CREATE EMPTY FULL-WIDTH GRID ****************************
fun createGrid(table: LinearLayout, rows: Int, cols: Int) : List<ViewGroup> {

        table.weightSum = rows.toFloat()


        // get a set of table cell children
        val cells: MutableList<ViewGroup> = mutableListOf()

        (0..rows - 1).map {

            // add table rows with layout-weight = 1
            val row = LinearLayout(context)
            table.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1.toFloat()))

            // set row weightSum to total columns
            row.weightSum = cols.toFloat()

            row.orientation = LinearLayout.HORIZONTAL

            row

        }.onEach {
            val row = it

            // add all cells to row
            (0..cols - 1).onEach {
                val fr = FrameLayout(context)

                // set cell params to match parent and weight = 1
                fr.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT, 1.toFloat())

                // add cell to row
                row.addView(fr)

                cells.add(fr)
            }
        }

        return cells.toList()
    }

    // returns list of left, top, right, bottom
    fun getMargins(width: Int, height: Int) : List<Int> {
        if (parent == null) {
            Log.w(TAG, "can't calculate margins: parent is null")
            return listOf(2,2,2,2)
        }
       val cellWidth: Float = (width/cols).toFloat()
        val cellHeight: Float = (height/rows).toFloat()
        val marginLeft = Math.max(cellWidth * margins/100, 1f).toInt()
        val marginTop = Math.max(cellHeight * margins/100, 1f).toInt()

        Log.d(TAG, "getting margins for "+ cellWidth+"x" + cellHeight+": " +
                "left:" + marginLeft + " top:" + marginTop)
        return listOf(marginLeft, marginTop, marginLeft, marginTop)
    }

}