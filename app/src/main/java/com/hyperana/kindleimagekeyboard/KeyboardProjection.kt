package com.hyperana.kindleimagekeyboard

/**
 * Created by alr on 11/17/17.
 * todo: -?- this could be a simple map with icons held in indexed array
 */
abstract class KeyboardProjection {

    abstract fun project(pages: List<PageData>) : List<PageData>
}