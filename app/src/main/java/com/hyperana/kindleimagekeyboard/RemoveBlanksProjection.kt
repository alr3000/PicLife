package com.hyperana.kindleimagekeyboard

import android.content.ContentValues.TAG
import android.util.Log
import java.io.File

/**
 * Created by alr on 12/8/17.
 * Remove icons with blank (small-file) images or default text if provided
 */
class RemoveBlanksProjection (val removeIfText: String? = null) : KeyboardProjection() {
    override fun project(pages: List<PageData>): List<PageData> {
        Log.d(TAG, "project")

        pages.onEach {
            val p = it
            p.icons = p.icons.filter {
                (((it.text != removeIfText) &&
                        ((it.path != null) && !File(it.path).exists())) &&
                        ( true )) //
            }.toMutableList()
        }

        return pages
    }
}