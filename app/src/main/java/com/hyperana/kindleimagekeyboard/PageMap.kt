package com.hyperana.kindleimagekeyboard



class PageMap {


    val TAG = "PageMap"

    val pageNavigator: PageNavigator = TwoAxisPageNavigator()
    val pageHistory: PageHistoryNavigator = object : PageHistoryNavigator {

        // list of visited pages, in order added:
        var list: List<PageData> = listOf()
        var currentIndex: Int = -1

        // clear forward portion of list and add new page to the new end:
        override fun add(page: PageData) {
            val newIndex = currentIndex + 1
            list = list.subList(0, newIndex).plus (page)
            currentIndex = newIndex
        }

        override fun clear() {
            list = emptyList()
            currentIndex = -1
        }

        override fun back(): PageData? {
            val newIndex =currentIndex - 1
            return moveToIndex(newIndex)
        }

        override fun forward(): PageData? {
            val newIndex = currentIndex + 1
            return moveToIndex(newIndex)
        }

        override fun listHistory(): List<PageData> {
            return list
        }

        override fun canGoBack(): Boolean {
            return currentIndex > 0
        }

        override fun canGoForward(): Boolean {
            return currentIndex + 1 < list.size
        }

        private fun moveToIndex(i: Int) : PageData? {
            return list.getOrNull(i)
        }
    }


}