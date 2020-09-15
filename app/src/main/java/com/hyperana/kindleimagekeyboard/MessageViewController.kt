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
    val inputter: WordInputter? = null,
    val iconListModel: IconListModel? = null,
    val overlay: ViewGroup,
    val backspaceView: View? = null,
    val forwardDeleteView: View? = null,
    val messageViewContainer: View? = null,
    val actionManager: ActionManager
) : ActionManager.ActionListener, Toolbar.OnMenuItemClickListener
{
    val TAG = "MessageViewController"


    //todo: use create view to get actionView and apply click listener (don't use menu click)
    val TITLE: AACAction = object: AACAction("title", "") {

    }
    val activeActions: List<AACAction>
        get() = listOf(
            AACAction.TOGGLE(false), SPEAK, CLEAR
        )
    val inactiveActions: List<AACAction>
        get() = listOf(
            TITLE, TOGGLE(true), SPEAK
        )

    //todo: separate logic for messageviewcontroller with wordinputter vs. iconlistmodel or always use iconlistmodel
    val inputActions: List<AACAction>
        get() = iconListModel?.let { actionManager.getActionsByListener(it)} ?: listOf(
            BACKSPACE, CLEAR
        )

    var selectedIndex = 0
    val iconListView: ViewGroup? = messageViewContainer?.findViewById<ViewGroup>(R.id.message_iconlist)
    val messageToolbar: Toolbar? = messageViewContainer?.findViewById(R.id.message_action_toolbar)


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
        iconListView?.viewTreeObserver?.addOnGlobalLayoutListener {
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
        messageToolbar?.apply {
            setOnMenuItemClickListener(this@MessageViewController)
        }
        lifecycleOwner.lifecycle.addObserver(object: LifecycleObserver {
            @OnLifecycleEvent(value = Lifecycle.Event.ON_START)
            fun onStart() {

                Log.d(TAG, "onStart")
              //  messageToolbar?.inflateMenu(R.menu.menu_main)
                updateMessageActions(if (iconListView?.visibility == View.VISIBLE)
                activeActions else inactiveActions)
            }
        })

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
            AACAction.CLEAR -> {
                iconListModel?.clear()
                return true
            }
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
        updateMessageActions(if (show) activeActions else inactiveActions)
    }

    // must do after drawing toolbar
    fun updateMessageActions(actions: List<AACAction>) {

        Log.d(TAG, "adding message actions: ${actions.joinToString()} to ${messageToolbar?.menu}")
        messageToolbar?.menu?.apply {
            removeGroup(getActionTag())
            actions.forEach {
                this.add(getActionTag(), it.menuId, 0, it.displayString).apply {
                    it.drawableId?.also { setIcon(it) }
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
            }
        }
    }
}