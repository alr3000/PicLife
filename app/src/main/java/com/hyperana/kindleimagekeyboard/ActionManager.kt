package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import com.google.common.collect.HashMultimap
//todo: this could be cascading linked list type thing with parents/children
class ActionManager(val lifecycle: Lifecycle) {

    val TAG = "ActionManager"
    private val actions: HashMultimap<AACAction, ActionListener> = HashMultimap.create()

    fun registerActionListener(l: ActionListener, list: List<AACAction>) {
        list.forEach { action ->
            actions.put(action, l)
        }
    }

    fun unregisterActionListener(l: ActionListener) {
        actions.keys().forEach { actions.remove(it, l) }
    }

    fun getListenersByAction(action: AACAction) : List<ActionListener> {
        return actions.get(action).toList()
    }

    fun getActionsByListener(l: ActionListener) : List<AACAction> {
        return actions.keys().filter { actions.get(it).contains(l)}
    }

    fun handleActionMenuId(itemId: Int, data: Any?) : Boolean {
        return actions.keys().find{ it.menuId == itemId }
            ?.let { handleAction(it, data) }
            ?: false
    }

    fun handleAction(action: AACAction, data: Any?) : Boolean {
        if (lifecycle.currentState != Lifecycle.State.RESUMED) {
            Log.w(TAG, "action requested while activity state = ${lifecycle.currentState}")
            return false
        }
        actions.get(action)?.let { list ->
            if (list.isEmpty()) return false
            list.forEach { it.handleAction( action, data )}
        }
        return true
    }

    interface ActionListener {
        fun handleAction(action: AACAction, data: Any?) : Boolean
        fun getActionTag() : Int
    }
}

open class AACAction(
    val id: String = "AACAction.Go",
    val displayString: String = "Go",
    val drawableId: Int? = null
) {
    val menuId: Int
        get() { return id.hashCode() }

    var data: Any? = null

    open fun createView(context: Context) : View {
        return Button(context).apply {
                drawableId
                    ?.also {
                        setBackgroundResource(it)
                        contentDescription = displayString
                    }
                    ?: run {text = displayString}
                tag = this@AACAction
            }
    }


    override fun toString(): String {
        return super.toString() + ": $displayString"
    }
    companion object {
        // data is view:
        val HIGHLIGHT = AACAction("highlight", "Highlight")

        // data is list of IconData:
        val PREVIEW = AACAction("AACAction.IconPreview", "Preview")
        val EXECUTE = AACAction("AACAction.IconExecute", "Input")

        // data is String:
        val SPEAK = AACAction("AACAction.MessageSpeak", "Speak")

        // data is null:
        val CLEAR = AACAction("AACAction.MessageClear", "Clear", R.drawable.clearallbutton)
        val BACKSPACE = AACAction("backspace", "<X", R.drawable.backspacebutton)
        val SCAN = AACAction("scan", "Scan")
        val SHOW = AACAction("show", "Show", android.R.drawable.arrow_down_float)
        val HIDE = AACAction("hide", "Hide", android.R.drawable.arrow_up_float)
        val OPEN_SETTINGS = AACAction("open_settings", "Settings", R.drawable.preferences)
        val EDIT = AACAction("edit","Edit")
        val HOME = AACAction("home", "Home", R.drawable.homebutton)
        fun TOGGLE(isHidden: Boolean) = if (isHidden) SHOW else HIDE
    }
}


