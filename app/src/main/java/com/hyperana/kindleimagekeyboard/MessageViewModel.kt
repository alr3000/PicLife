package com.hyperana.kindleimagekeyboard

import androidx.lifecycle.*
import kotlin.math.min

interface IconProvider {
    fun observeIcons(lifecycleOwner: LifecycleOwner, observer: Observer<List<IconData>>)
}


class MessageViewModel: ViewModel(), IconProvider {
    val TAG = "MessageViewModel"

    private val icons = MutableLiveData<List<IconData>>(listOf())
    fun getIcons() : LiveData<List<IconData>> {
        return icons
    }
    fun setIcons(list: List<IconData>?) {
        icons.value = list
    }
    fun appendIcon(icon: IconData) {
        setIcons((icons.value ?: listOf()).plus(icon))
    }
    fun removeIconAt(index: Int) {
        val length = icons.value?.size ?: 0
        if (length > index)
            icons.value = icons.value?.subList(0, index)?.plus(
                icons.value!!.subList(min(length - 1, index + 1), length)
            )
        }

    fun addIconAt(icon: IconData, index: Int) {
        val length = icons.value?.size ?: 0
        if (index >= length) appendIcon(icon)
        else icons.value = icons.value!!.subList(0, index)
            .plus(icon)
            .plus(icons.value!!.subList(index, length))
    }

    override fun observeIcons(lifecycleOwner: LifecycleOwner, observer: Observer<List<IconData>>) {
        getIcons().observe(lifecycleOwner, observer)
    }




}
