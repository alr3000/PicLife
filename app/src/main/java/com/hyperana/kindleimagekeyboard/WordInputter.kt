package com.hyperana.kindleimagekeyboard

/**
 * Created by alr on 9/15/17.
 *
 */
interface WordInputter  {


    // todo: delete selection if any, clear

    abstract fun input(text: String)
    fun input(icon: IconData)

    abstract fun forwardDelete()

    abstract fun backwardDelete()

    abstract fun getAllText() : String

    abstract fun action()


}