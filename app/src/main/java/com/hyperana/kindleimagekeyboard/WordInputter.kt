package com.hyperana.kindleimagekeyboard

/**
 * Created by alr on 9/15/17.
 */
abstract class WordInputter () {

    val TAG = "WordInputter"
    val wordbreak = Regex("\\W")
    val word = Regex("[^\\W]+")

    var textListener: InputListener? = null
    interface InputListener {
        fun onTextChanged(text: String)
    }


    data class RelativeWord(var text: String = "", var start: Int = 0, var endInclusive: Int = 0) {
        override fun toString() : String {
            return "RelativeWord (text: " + text + ", start: " + start.toString() + ", endInclusive: " + endInclusive.toString()
        }
    }

    // subclasses should call this function any time text is changed or action taken and on start
    fun update() {
        textListener?.onTextChanged(getAllText())
    }

    fun splitWords(text: CharSequence) : List<String> {
        return wordbreak.split(text, 0)
    }
    // todo: delete selection if any

    abstract fun input(text: String)

    abstract fun forwardDelete()

    abstract fun backwardDelete()

    abstract fun getAllText() : String

    abstract fun action()



}