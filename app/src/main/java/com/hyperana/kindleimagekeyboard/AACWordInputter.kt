package com.hyperana.kindleimagekeyboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// Provides a bridge from an icon list view model to an edited text string with cursor
class AACWordInputter(model: IconListModel) : WordInputter {


//todo: input Icondata
    override fun input(icon: IconData) {

    }

    override fun input(text: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun forwardDelete() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun backwardDelete() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllText(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun action() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}