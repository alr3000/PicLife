package com.hyperana.kindleimagekeyboard

import android.util.Log

/**
 * Created by alr on 11/17/17.
 *
 * Returns List<PageData> with the following changes:
 * rows, cols, margins properties added to pagedata,
 * indexAdjusted added to icondata
 *
 * icon indices are altered as necessary to fit as many icons into the grid as possible
 * placed icons are given row and col numbers in data
 * unplaced icons are given "null" index and "hidden=true" in data
 *
 *
 * todo: -L- arrange icons to fill grid, retaining their relative positions
 * todo: insert pages to hold leftover icons in small grids -- related ids
 *
 */
class FittedGridProjection(val cols: Int, val rows: Int, var margins: Int? = 10) : KeyboardProjection() {
    val TAG = "FixedKeyboardProjection"



    override fun project(pages: List<PageData>): List<PageData> {
        pages.onEach {
            it.set("rows", rows.toString())
            it.set("cols", cols.toString())
            it.set("margins", margins.toString())
            mapPageIcons(it.icons, rows * cols)
        }
        return pages
    }

    //todo: bug - when grid is full, first remainder icon is set to index icons.count()
    fun mapPageIcons(icons: List<IconData>, gridCount: Int) {

        // Sets to null values that are not parsable or are duplicated upstream. Returns unique values.
        fun findUniqueIndices(list: List<IconData>) : MutableList<Int> {
            val used: MutableList<Int> = mutableListOf()
            list.onEach {
                val index: Int? = it.index?.toIntOrNull()
                if ((index != null)  && (!used.contains(index))) {
                    used.add(index)
                }
            }
            return used
        }

        // sort used indices ascending
        val taken = findUniqueIndices(icons).sorted().toMutableList()
        Log.d(TAG, "resolving indices: found " + taken.joinToString(", "))

        // take first (lowest)
        var reserved = if (taken.isEmpty()) -1 else taken.removeAt(0)
        var next = 0

        icons.onEach{
            try {
                it.set("indexAdjusted",it.index)

                val i = it.index?.toIntOrNull()

                // reassign icons with null or out-of-bounds indices, cut off remainder
                if ((i == null) || (i >= gridCount)) {
                    if (next < gridCount) { // else no more room, so icon is left out
                        // jump over taken indices
                        while (next == reserved) {
                            next++
                            if (next > reserved) {
                                reserved = if (taken.isEmpty()) reserved else taken.removeAt(0)
                            }
                        }

                        Log.d(TAG, "Set icon " + i + " to index " + next)
                        it.set("indexAdjusted", next.toString())
                        next++
                    }
                    else {
                        it.set("indexAdjusted", null)
                    }
                }

                // pass through appropriately indexed icons unchanged
 /*               if (it.index != null) {
                    it.set("row", (it.index!!.toInt() / cols).toString())
                    it.set("col", (it.index!!.toInt() % cols).toString())
                }
 */           }
            catch (e: Exception) {
                Log.e(TAG, "failed to map icon", e)
            }

        }
    }

}