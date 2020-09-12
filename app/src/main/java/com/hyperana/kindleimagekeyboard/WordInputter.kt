package com.hyperana.kindleimagekeyboard

import java.util.*

/**
 * Created by alr on 9/15/17.
 *
 */
interface WordInputter {


    // todo: delete selection if any, clear

    abstract fun input(text: String)
    abstract fun input(icon: IconData)

    abstract fun forwardDelete()

    abstract fun backwardDelete()

    abstract fun getAllText() : String


    abstract fun setIndex(i: Int?)
    abstract fun moveIndex(num: Int)

    abstract fun clear()


}