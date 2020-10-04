package com.hyperana.kindleimagekeyboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlin.math.max
import kotlin.math.min

//todo: word inputter contains iconlistmodel and implements interface as below
open class IconListModel: ViewModel(), WordInputter, ActionListener {
    open val TAG = "IconListModel"

    // icons value is always non-null. Set iconsLiveData, get icons.
    private val iconsLiveData = MutableLiveData<List<IconData>>(listOf())
    val icons = Transformations.map(iconsLiveData) {
        it ?: listOf()
    }

    // index value returned is always non-null, in (0 .. icons.size)
    private val indexLiveData = MutableLiveData<Int>(0)
    val index = Transformations.map(indexLiveData) {
        val max = icons.value!!.size
        max(min(it ?: max, max), 0)
    }

    private val eventLiveData = MutableLiveData<AACAction?>(null)
    val event: LiveData<AACAction?>
            get() = eventLiveData

    private fun appendIcon(icon: IconData) {
        addIconAt(icon, iconsLiveData.value?.size ?: 0)
    }
    private fun removeIconAt(position: Int) {
        val length = icons.value!!.size
        Log.d(TAG, "removeIconAt $position/$length")
        if (position in 0 until length)

        // use slice to copy icons before and after removed:
            iconsLiveData.value = icons.value!!.slice(0 until position).plus(
                if (position + 1 == length) emptyList() else
                icons.value!!.slice(position + 1 until length)
            )
    }
    private fun addIconAt(icon: IconData, position: Int) {
        Log.d(TAG, "addiconAt $position")
        val length = icons.value?.size ?: 0
        iconsLiveData.value = icons.value!!.subList(0, position)
            .plus(icon)
            .plus(icons.value!!.subList(position, length))
    }

    fun getIconsText(list: List<IconData>) : String {
        return list.map { it.text }.joinToString (" ")
    }


    override fun handleAction(action: AACAction, data: Any?): Boolean {
        when (action) {
            AACAction.CLEAR -> clear()
            AACAction.BACKSPACE -> backwardDelete()
            AACAction.EXECUTE -> (data as? List<*>)?.forEach {
                (it as? IconData)?.also { input(it) }
            }
        }
        return true
    }

    override fun getActionTag(): Int {
        return hashCode()
    }

    // WordInputter Interface
    override fun setIndex(i: Int?) {
        Log.d(TAG, "setIndex $i")
        val length = icons.value!!.size
        indexLiveData.value = i?.coerceIn(0 .. length) ?: length
    }
    override fun moveIndex(num: Int) {
        setIndex(index.value?.plus(num))
    }


    override fun input(text: String) {
        Log.d(TAG, "input text: $text")
        this.input(IconData().apply {
            this.text = text
        })
    }

    override fun input(icon: IconData) {
        addIconAt(icon, index.value!!)
        moveIndex(1)
    }

    // removes icon ahead of cursor, if any
    override fun forwardDelete() {
        removeIconAt(index.value!!)
        //index doesn't change
    }

    // removes icon behind cursor, if any
    override fun backwardDelete() {
        removeIconAt(index.value!! - 1)
        moveIndex(-1)
    }

    override fun getAllText(): String {
        return getIconsText(icons.value!!)
    }



    override fun clear() {
        iconsLiveData.value = emptyList()
        setIndex(0)
    }
}
