package com.hyperana.kindleimagekeyboard

import android.graphics.Rect
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.*
import com.hyperana.kindleimagekeyboard.AACAction.Companion.BACKSPACE
import com.hyperana.kindleimagekeyboard.AACAction.Companion.CLEAR
import com.hyperana.kindleimagekeyboard.AACAction.Companion.HIDE
import com.hyperana.kindleimagekeyboard.AACAction.Companion.SHOW
import com.hyperana.kindleimagekeyboard.AACAction.Companion.SPEAK
import com.hyperana.kindleimagekeyboard.AACAction.Companion.TOGGLE

/*

interface MessageViewManager {
    var messageText: String?
    var showMessage: Boolean
    var expandMessage: Boolean
    fun highlightMessage()
    fun scanMessage()
    fun updateMessage(list: List<IconData>)
}
*/



// ime uses inputter, main activity uses iconListModel(which is also a WordInputter)
// todo: break this out into input controller and message view holder
class MessageViewController (
    val app: App,
    val lifecycleOwner: LifecycleOwner,
    val iconListModel: IconListModel? = null,
    val messageViewContainer: View? = null,
    val actionManager: ActionManager
) : ActionListener, Toolbar.OnMenuItemClickListener
{
    val TAG = "MessageViewController"


    //todo: use create view to get actionView and apply click listener (don't use menu click)
    val TITLE: AACAction = object: AACAction("messageText", "") {

    }
    // actions shown while message iconlistview is visible:
    val activeActions: List<AACAction>
        get() = listOf(
            SPEAK, CLEAR
        )
    // actions shown when message view is collapsed:
    val inactiveActions: List<AACAction>
        get() = listOf(
            TITLE, SPEAK
        )

    var selectedIndex = 0
    val iconListView: ViewGroup? = messageViewContainer?.findViewById<ViewGroup>(R.id.message_iconlist)
    val messageToolbar: ActionToolbar? = messageViewContainer?.findViewById<Toolbar>(R.id.message_action_toolbar)
        ?.let { ActionToolbar(it) }


    // match icon list to message model:
    val messageObserver = object: Observer<List<IconData>> {
        override fun onChanged(t: List<IconData>?) {
            try {

                //todo: highlight action if not empty?
                updateMessage(t ?: emptyList())


            } catch (e: Exception) {
                Log.w(TAG, "failed done button highlight", e)
            }
        }
    }

    // update cursor position:
    val messageCursorObserver = object: Observer<Int> {
        override fun onChanged(t: Int?) {
            Log.d(TAG, "cursor at: $t")
            val i = t?.let { (it - 1).coerceAtLeast(0) } ?: 0
            selectedIndex = i
        }
    }



    init {

        // observe message model:
        iconListModel?.icons?.observe(lifecycleOwner, messageObserver)
        iconListModel?.index?.observe(lifecycleOwner, messageCursorObserver)


        // observe layout:
        iconListView?.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            Log.d(TAG, "layout listener")

            // keep selected icon (cursor position ) in view:
            val rect = Rect()
            iconListView.getChildAt(selectedIndex)?.also { icon ->
                icon.getLocalVisibleRect(rect)
                Log.d(TAG, "focusing rect $rect")
                icon.requestRectangleOnScreen(rect)
            }
        }

        // add action views:
        updateToolbar()
    }

    fun updateToolbar() {
        messageToolbar?.replaceActions(this, if (iconListView?.visibility == View.VISIBLE)
            activeActions else inactiveActions)
        messageToolbar
            ?.setLeftCornerAction(this, AACAction.TOGGLE(iconListView?.visibility != View.VISIBLE))
    }



    // message interface:
    fun updateMessage(list: List<IconData>) {
        Log.d(TAG, "update message view (${list.map { it.text }.joinToString("-")}")

        iconListView?.also { container ->
            container.removeAllViews()
            list.forEach {
                container.addView(
                    IconData.createView(it, container.context, true)
                )
            }
        }
    }



    override fun onMenuItemClick(item: MenuItem?): Boolean {
        item ?: return false
        return activeActions.plus(inactiveActions).find { it.menuId == item.itemId }
            ?.let { handleAction(it, null) }
            ?: false
    }

    override fun handleAction(action: AACAction, data: Any?): Boolean {
        Log.i(TAG, "handle action: $action, $data")
        when (action) {
            HIDE -> toggleView(false)
            SHOW -> toggleView(true)
            CLEAR -> iconListModel?.handleAction(action, null)
            SPEAK -> actionManager.handleAction(action, iconListModel?.getAllText())
        }
        return false
    }

    override fun getActionTag(): Int {
        return TAG.hashCode()
    }

    // action handler methods:
    fun toggleView(show: Boolean) {
        iconListView?.visibility = if (show) View.VISIBLE else View.GONE

        // update actionview when hidden/shown:
        updateToolbar()
    }

}