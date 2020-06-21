package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.graphics.drawable.shapes.Shape
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.WindowManager
import android.widget.*

/**
 * Created by alr on 9/17/17.
 *
 * View duplicates original view, then animates to final size and position:
 *      zoomed by given highlightZoom percentage,
 *      offset to show above or below original view, within the bounds of the screen.
 *
 * Translation is set relative to root view, so that is where highlightView should be attached.
 *
 *
 */
// todo: -L- map of options instead of app reference
class HighlightView (val originalView: View, val insertView: ImageView?, val app: App)
    : FrameLayout(originalView.context){
    val TAG = "HighlightView"

    val startRect = Rect()
    val parentRect = Rect()

    var placement = "in place"
    var border =  8
    var totalTime = 300L
    var zoomPercent: Float = 100f

    val colorStr = app.get("highlightColor")?.toString() ?: "#FFFF00"


    // returns a value within the range
    fun fit(value: Float, min: Float, max: Float) : Float {
        return when {
            (value < min) -> min
            (value > max) -> max
            else -> value
        }
    }

    // add this amount to a value to fit it into a range
    fun clip(value: Float, min: Float, max: Float) : Float {
        return if (value > max) max - value
        else if (value < min) min - value
        else 0f
    }

    init {

        if (insertView != null) {
            // use icon-highlight settings
            placement = app.get("highlightPlace")?.toString() ?: placement
            border = app.get("highlightBorder")?.toString()?.toIntOrNull() ?: border
            totalTime = app.get("highlightTime")?.toString()?.toLongOrNull() ?: totalTime
            zoomPercent = app.get("highlightZoom")?.toString()?.toFloatOrNull() ?: zoomPercent
        }

        //todo: figure out the new background drawable methods
        val outline = context.resources.getDrawable(R.drawable.mask_roundcorners)
        outline.setColorFilter(Color.parseColor(colorStr), PorterDuff.Mode.SRC_IN)
        setBackgroundDrawable(outline)

        //setBackgroundColor(Color.parseColor(colorStr))
        setPadding(border, border, border, border)

        Log.d(TAG, "highlightView settings: " + totalTime.toString() + "ms," + zoomPercent + "%," + border )

        if (insertView != null) {
            insertView.scaleType = ImageView.ScaleType.FIT_XY
            addView(insertView)
        }

        // delay rest of initialization until parent is known
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                Log.d(TAG, "onViewDetached")
            }

            override fun onViewAttachedToWindow(v: View?) {
                try {
                    Log.d(TAG, "onViewAttached")

                    // initialize position once we have the parent
                    initializeStartRect()
                    translationX = startRect.left.toFloat()
                    translationY = startRect.top.toFloat()

                    doAnimation()
                }
                catch (e: Exception) {
                    Log.e(TAG, "problem on attaching highlightView", e)
                }
             }
        })

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
         try {
            setMeasuredDimension(startRect.width(), startRect.height())
            super.onMeasure(
                    makeMeasureSpec(startRect.width(), MeasureSpec.EXACTLY),
                    makeMeasureSpec(startRect.height(), MeasureSpec.EXACTLY)
            )
        }
        catch (e: Exception) {
            Log.e(TAG, "problem measuring highlightView", e)
        }
    }

   /* override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        try {
            Log.d(TAG, "onLayout")
            super.onLayout(true, startRect.left, startRect.top, startRect.right, startRect.bottom)
           // doAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "problem on layout of highlight for " + icon.text, e)
        }
    }
*/
    // starts right on top of original view, expanded by "border" to let padding show
    fun initializeStartRect() {

        originalView.getGlobalVisibleRect(startRect)
        (parent as View).getGlobalVisibleRect(parentRect)

        Log.d(TAG, "highlight positioning.. ")
        Log.d(TAG, "highlight parent in window: " + parentRect.toString())
        Log.d(TAG, "original in window: " + startRect.toString())

        // find original view in highlight parent coords
        startRect.offset(0 - parentRect.left, 0 - parentRect.top)

        // outset to allow for border (padding)
        startRect.inset(-border, -border)

        Log.d(TAG, "startRect (start): " + startRect.toString())

    }

    fun doAnimation() {

        // mock up final zoomed rect...
        val fWidth = originalView.width*(zoomPercent/100) + border*2
        val fHeight = originalView.height*(zoomPercent/100) + border*2
        val dW = fWidth - startRect.width()
        val dH = fHeight - startRect.height()

        // ...centered in place
        var dX = 0
        var dY = 0

       // adjust for different placement settings
        Log.d(TAG, "placement = " + placement)
        when (placement) {
            "in place" -> {}
            "in center" -> {
                dX = parentRect.centerX() - startRect.centerX()
                dY = parentRect.centerY() - startRect.centerY()
            }
            "above key" -> {
                dY = -((startRect.height() + fHeight)/2).toInt()
            }
        }

        // adjust to keep it in parent
        dX  += clip(startRect.left + dX - dW/2, 0.2f, parentRect.width() - fWidth).toInt()
        dY  += clip(startRect.top + dY - dH/2, 0.2f, parentRect.height() - fHeight).toInt()

        // animate to new size and position
        Log.d(TAG, "animate highlight by: " + dW + "x" + dH + "  (" + dX + "," + dY+ ")")
        val animator = this.animate()

        // scale automatically keeps center in place!
        animator.scaleX(fWidth/startRect.width())
        animator.scaleY(fHeight/startRect.height())
        animator.xBy(dX.toFloat())
        animator.yBy(dY.toFloat())
        animator.duration = totalTime/3
    }

}