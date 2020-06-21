package com.hyperana.kindleimagekeyboard

import android.util.Log

/**
 * Created by alr on 11/17/17.
 *
 * //todo: -L- created icons persist of course in shared pagelist when link settings change
 * //-- tag and destroy or rebuild pagelist or get copy for personal use?
 * // icon creating settings removed
 */
class LinkedPagesProjection(val linkMode: String): KeyboardProjection() {
    val TAG = "LinkedPagesProjection"

    // uses found icons or adds new ones to create links to subpages, depending on mode
    override fun project(pages: List<PageData>): List<PageData> {
        Log.d(TAG, "project: " + linkMode)

        pages.onEach{
            val to = it
            if (it.parentPageId != null) {

                // find child's parent page and then find/create an icon to link to child
                // todo: -?- pages are mapped by id - this can be quite long...
                val from = pages.find { it.id == to.parentPageId }
                if (from != null) {
                    val linkIcon = createLink(linkMode, to, from)
                    to.set("parentIconId", linkIcon?.id)
                }
            }
        }
        return pages
    }



    fun createLink(linkMode: String?, to: PageData, from: PageData) : IconData? {
        val linkIcon = when (linkMode) {
            "createLinksMatching" -> {
                // find matching icon or null
                from.icons.find{ it.text?.toLowerCase() == to.name!!.toLowerCase() }
            }
           /* "createLinksMissing" -> {
                // add if no match
                from.icons.find{ it.text?.toLowerCase() == to.name!!.toLowerCase() }
                        ?: IconData(text = iconFilenameToText(to.name!!))
                        .also {
                             from.icons.add(it)
                        }
            }
            "createLinksDuplicate" -> {
                // add no matter what and use matching icon image if present
                val img = from.icons.find { it.text?.toLowerCase() == to.name!!.toLowerCase() }
                IconData(text = iconFilenameToText(to.name!!))
                        .also {
                            it.path = img?.path
                            from.icons.add(it)
                        }
            }*/
            else -> {
                // no link
                null
            }
        }
        linkIcon?.linkToPageId = to.id
        Log.d(TAG, "link from " + from.name + " to " + to.name + " is " + linkIcon?.id)

        return linkIcon
  }
}