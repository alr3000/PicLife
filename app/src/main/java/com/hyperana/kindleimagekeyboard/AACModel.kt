package com.hyperana.kindleimagekeyboard


// This model is built from keyboard id and a few preferences
// includes a cache-list of all pages in aac and helpers
// projects pages according to settings and produces final page navigator
class AACModel(
    val allPages: List<PageData>,
    val keyboard: Keyboard?,
    private val navigator: PageNavigator,
    private val state: ObservableNavigationState
) : PageNavigator by navigator, ObservableNavigationState by state {

    val TAG = "AACModel"

    fun goToHome() : PageData? {
        return allPages.firstOrNull()
    }

    class Builder(val repository: AACRepository) {
        var keyboardId: PageId? = null
        var upList: List<PageId> = emptyList()
        var downList: List<PageId> = emptyList()
        var projection: (List<PageData>) -> List<PageData> = { list -> list }
        var observableState: ObservableNavigationState = LiveDataNavigationState()


        suspend fun create(): AACModel {

            val aacPages: List<PageData> = (repository.asyncBuildKeyboard(keyboardId)?.await() ?: emptyList())
                .let { projection?.invoke(it) ?: it }

            val upPages: List<PageData> = repository.getAllById(upList.toIntArray()).map { PageData(it) }
            val downPages: List<PageData> = repository.getAllById(upList.toIntArray()).map { PageData(it) }
            val keyboard =  keyboardId?.toInt()?.let { Keyboard( repository.getLiveResource(it)) }
            val nav = TwoAxisPageNavigator(
                throughList = aacPages.map { it.id.toInt() }, //todo: resourceid is string
                altList = upList.plus(TwoAxisPageNavigator.NEUTRAL_PLACEHOLDER).plus(downList),
                observableState
            )

            return AACModel(
                upPages.plus(downPages).plus(aacPages),
                keyboard,
                nav,
                observableState // shared with navigator
            )

        }

    }
}


