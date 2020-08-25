package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.preference.PreferenceManager
import android.util.AttributeSet
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


class InputPageView(context: Context, attributeSet: AttributeSet?) : LinearLayout(context, null){

    constructor(
        context: Context,
         pPage: PageData,
         pColor: Int) : this(context, null) {

        page = pPage
        color = pColor

    }
    var page: PageData = PageData()
    var color: Int = Color.GRAY

    val TAG = "InputPageView - " + page.id

    val app = App.getInstance(context.applicationContext)

    var views: List<ViewGroup> = listOf()
    var items: List<IconData> = page.icons

    var margins = 10 // percent of cellwidth
    var rows: Int = 3
    var cols: Int = 5
    val touchHandler = IconPageTouchHandler(app.iconEventLiveData)
    val touchAction = app.get("touchAction") as? String

    interface PageListener {
        fun preview(icon: IconData?, view: View?)
        fun execute(icon: IconData?, view: View?)
    }

    init {

        // prepare page styles todo: cascading from app, then page overrides
        margins = page.get("margins")?.toIntOrNull() ?: margins
         cols = page.get("cols")?.toInt() ?: cols
        rows = page.get("rows")?.toInt() ?: rows

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

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        try {
        return touchHandler.onTouchEvent(this, views, event, touchAction)
        } catch (e: Exception) {
            Log.e(TAG, "problem with icon touch handler: ", e)
            return false
        }
    }



    // depending on selected icon activation type,
    // returns whether event is of interested in future events
    // todo: touch handler is separately defined, takes getResolveIcon(x, y) function

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