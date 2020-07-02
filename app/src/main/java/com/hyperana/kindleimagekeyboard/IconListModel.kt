package com.hyperana.kindleimagekeyboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlin.math.max
import kotlin.math.min

//todo: word inputter contains iconlistmodel and implements interface as below
class IconListModel: ViewModel(), WordInputter {
    val TAG = "IconListModel"

// todo: delete selection if any, clear
    //todo: make a factory to create this from given text

    // icons value is always non-null. Set iconsLiveData, get icons.
    private val iconsLiveData = MutableLiveData<List<IconData>>(listOf())
    val icons = Transformations.map(iconsLiveData) {
        it ?: listOf()
    }.apply { observeForever({}) }

    // index value returned is always non-null, in (0 .. icons.size)
    private val indexLiveData = MutableLiveData<Int>(0)
    val index = Transformations.map(indexLiveData) {
        val max = icons.value!!.size
        max(min(it ?: max, max), 0)
    }.apply { observeForever({}) }


    private fun appendIcon(icon: IconData) {
        iconsLiveData.value = icons.value!!.plus(icon)
    }
    private fun removeIconAt(index: Int) {
        val length = icons.value!!.size
        if (length > index)
            iconsLiveData.value = icons.value!!.subList(0, index).plus(
                icons.value!!.subList(min(length - 1, index + 1), length)
            )
    }
    private fun addIconAt(icon: IconData, position: Int) {
        val length = icons.value?.size ?: 0
        if (position >= length) appendIcon(icon)
        else {
            iconsLiveData.value = icons.value!!.subList(0, position)
                .plus(icon)
                .plus(icons.value!!.subList(position, length))
            if (position <= index.value!!) moveIndex(1)
        }
    }


    // WordInputter Interface
    override fun setIndex(i: Int?) {
        indexLiveData.value = i
    }
    override fun moveIndex(num: Int) {
        setIndex(index.value?.plus(num))
    }


    override fun input(text: String) {
        input(IconData().apply {
            this.text = text
        })
    }

    override fun input(icon: IconData) {
        addIconAt(icon, index.value!!)
        moveIndex(1)
    }

    override fun forwardDelete() {
        removeIconAt(index.value!!)
        //index doesn't change
    }

    override fun backwardDelete() {
        removeIconAt(index.value!! - 1)
        moveIndex(-1)
    }

    override fun getAllText(): String {
        return icons.value!!
            .map { it.text }
            .joinToString(" ")
    }

    override fun action() {
        // I don't know what to do here
    }
}
