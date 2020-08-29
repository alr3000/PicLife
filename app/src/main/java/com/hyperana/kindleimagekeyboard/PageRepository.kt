package com.hyperana.kindleimagekeyboard

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class PageRepository(db: AppDatabase) {

    private val resourceDao = db.resourceDao()

    var aacPageResources = resourceDao.getAllByType(arrayOf(Resource.Type.PAGE.name))

    var recentsPages = listOf(RecentsPage())
    var toolsPages = listOf(ToolsPage())



}