package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.os.Handler
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import java.io.File
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.widget.*
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
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
open class PageViewHolder(var view: InputPageView) {

    // no effect until page is set:
    var page: PageData = PageData()
        set(value) {
            field = value
            view.page = value
        }

}

// todo: merge this with IconListAdapter
open class IconAdapter(val context: Context, val icons: List<IconData>) : BaseAdapter() {
    val TAG = "IconAdapter"
    init { Log.d(TAG, "${icons.size} icons")}

    override fun getCount(): Int {
        return icons.size
    }

    override fun getItem(position: Int): Any? {
        return icons.getOrNull(position)
       /* return icons.find {
            it.get("indexAdjusted")?.toIntOrNull() == position
        }*/
    }

    override fun getItemId(position: Int): Long {
        return (getItem(position) as? IconData)?.id?.toLongOrNull() ?: -1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        return (getItem(position) as? IconData)?.let { IconData.createView(it, context) }
    }
}



class InputPageView(context: Context, attributeSet: AttributeSet? = null) :
    LinearLayout(context, attributeSet) {

    //todo: attach to view holder with model
    var page: PageData = PageData().apply { name = "Empty page" }
    set(value) {
        Log.d(TAG, "setting page ${value.name}")
        field = value
        iconAdapter = IconAdapter(context, value.icons)
        setStyle(value)
        setViews()
    }
    // todo: cascading styles
    var color: Int = Color.DKGRAY
    var rows =  3
    var cols =  5
    var margins = 10 // percent of cellwidth



    val TAG = "InputPageView - "
    get() = field + page.name

    val app = App.getInstance(context.applicationContext)

    var iconAdapter: IconAdapter? = null

    // views are frames for icons, may not be immediate children of this view:
    var views: List<ViewGroup> = listOf()

    var touchHandler: IconPageTouchHandler? = null
    val touchAction = app.get("touchAction") as? String
    var actionManager: ActionManager? = null


    init {
        orientation = LinearLayout.VERTICAL
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        //setStyle()

        actionManager = (context as? MainActivity)?.actionManager
        touchHandler = IconPageTouchHandler(actionManager)

    }


    // todo: set with cascaded map of strings
    fun setStyle(page: PageData) {
        margins = page.get("margins")?.toIntOrNull() ?: margins
        rows = page.get("rows")?.toIntOrNull() ?: 3
        cols = page.get("cols")?.toIntOrNull() ?: 5
        color = page.get("backgroundColor")?.toIntOrNull() ?: Color.DKGRAY
    }

    fun setViews() {
        removeAllViews()

        // set page features
        setBackgroundColor(color)


        // add table rows and cells
        views = createGrid(
            table = this,
            rows = rows,
            cols = cols
        )

        // add icon views
         views.onEachIndexed { index, view ->

            try {
                iconAdapter!!.getView(index, null, view )
                    ?.also { view.addView(it) }
                    ?.also { view.tag = it.tag }
                    .also { Log.d(TAG, "setting $it at $index") }
            }
            catch (e: Exception) {
                Log.w(TAG, "failed set icon", e)
            }
        }

    }


    // set margins after size is known, and before layout so they take effect
    // shouldn't have to be done on refit - todo
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val marr = getMargins(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec))
        Log.d(TAG, "onMeasure: ${marr.joinToString()}")
        views.onEach {
            (it.layoutParams as? LinearLayout.LayoutParams)
                    ?.setMargins(marr[0], marr[1], marr[2], marr[3])
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        Log.d(TAG, "onLayout: ${iconAdapter?.count} icons")
    }

    //************************************* TOUCH HANDLERS ***************************************
    // settings: touchAction (touchActionDown, touchActionUp, touchActionClick)

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        try {
            return touchHandler!!.onTouchEvent(this, views, event, touchAction)
        } catch (e: Exception) {
            Log.e(TAG, "problem with icon touch handler: ", e)
            return false
        }
    }

    // depending on selected icon activation type,
    // returns whether event is of interested in future events

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