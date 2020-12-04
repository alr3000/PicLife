package com.hyperana.kindleimagekeyboard

import android.util.Log
import kotlinx.coroutines.Deferred


interface ResourceInflater {
    fun inflatePageAsync(id: PageId?): Deferred<PageData>
    fun inflateIconAsync(id: PageId?): Deferred<IconData>
    fun inflateKeyboardAsync(id: PageId?): Deferred<Keyboard>
}

// This model is built from keyboard id and a few preferences
// includes a cache-list of all pages in aac and helpers
// projects pages according to settings and produces final page navigator
class AACModel(
    val repository: ResourceInflater,
    val aacPages: List<PageData>,
    val keyboard: Keyboard?,
    private val navigator: PageNavigator,
    private val state: ObservableNavigationState
) : PageNavigator by navigator, ObservableNavigationState by state {

    val TAG = "AACModel"

    init {
        goToHome()
    }

    fun goToHome()  {
        Log.d(TAG, "gotohome")
        navigator.goToIndex(0)
    }

    class Builder() {
        var repository: AACRepository? = null
        var keyboardId: PageId? = null
        var upList: List<PageId> = emptyList()
        var downList: List<PageId> = emptyList()
        var projection: (List<PageData>) -> List<PageData> = { list -> list }
        var observableState: ObservableNavigationState = LiveDataNavigationState()


        //todo: use resource inflater to create pages and icons? or resource inflater is avail to all
        //todo: repository offers observable resources?
        suspend fun create(): AACModel {
            val keyboard = repository!!.inflateKeyboardAsync(keyboardId).await()
            val aacPages: List<PageData> = keyboard.pageList
                //.let { projection.invoke(it) }
            Log.i("AACModel.Builder", "create: kbd $keyboardId, with ${aacPages.size} pages")

            val nav = TwoAxisPageNavigator(
                throughList = aacPages.map { it.id.toInt() }, //todo: resourceid is string
                altList = upList.plus(TwoAxisPageNavigator.NEUTRAL_PLACEHOLDER).plus(downList),
                observableState
            )

            return AACModel(
                repository!!,
               aacPages,
                keyboard,
                nav,
                observableState // shared with navigator
            )

        }

    }
}


