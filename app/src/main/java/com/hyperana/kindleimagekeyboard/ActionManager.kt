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
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

interface ActionListener {
    fun handleAction(action: AACAction<*>, data: Any?) : Boolean
    fun getActionTag() : Int
}

class ActionManager(val lifecycle: Lifecycle): ActionListener {

    val TAG = "ActionManager"
    private val actions: HashMultimap<AACAction<*>, ActionListener> = HashMultimap.create()

    private var channel: Channel<Pair<AACAction<*>, Any?>> = Channel(10)

    fun registerActionListener(l: ActionListener, list: List<AACAction<*>>) {
        list.forEach { action ->
            actions.put(action, l)
        }
    }

    fun unregisterActionListener(l: ActionListener) {
        actions.keys().forEach { actions.remove(it, l) }
    }

    fun getListenersByAction(action: AACAction<*>) : List<ActionListener> {
        return actions.get(action).toList()
    }

    fun getActionsByListener(l: ActionListener) : List<AACAction<*>> {
        return actions.keys().filter { actions.get(it).contains(l)}
    }

    fun handleActionMenuId(itemId: Int, data: Any?) : Boolean {
        return actions.keys().find{ it.menuId == itemId }
            ?.let { handleAction(it, data) }
            ?: false
    }

    override fun handleAction(action: AACAction<*>, data: Any?) : Boolean {
        Log.i(TAG, "handleActions: $action, $data")
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

    override fun getActionTag(): Int {
        return hashCode()
    }
}

open class AACAction<T:Any?>(
    val id: String = "AACAction.Go",
    var displayString: String = "Go",
    var drawableId: Int? = null
) {
    val menuId: Int
        get() { return id.hashCode() }

    var data: T? = null
    fun transformData(d: Any?) : T? {
        return d as? T
    }

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
        val menuId = 0
        // data is view:
        val HIGHLIGHT = AACAction<View>("highlight", "Highlight")
        val FLASH = AACAction<View>("Flash", "Flash")

        // data is iconList:
        val PREVIEW = AACAction<List<IconData>>("AACAction.IconPreview", "Preview")
        val EXECUTE = AACAction<List<IconData>>("AACAction.IconExecute", "Execute")

        // data is String:
        val SPEAK = AACAction<String>("AACAction.MessageSpeak", "Speak")

        // data is null:
        val CLEAR = AACAction<Any?>("AACAction.MessageClear", "Clear", R.drawable.clearallbutton)
        val BACKSPACE = AACAction<Any?>("backspace", "<X", R.drawable.backspacebutton)
        val SHOW = AACAction<Any?>("show", "Show", android.R.drawable.arrow_down_float)
        val HIDE = AACAction<Any?>("hide", "Hide", android.R.drawable.arrow_up_float)
        val OPEN_SETTINGS = AACAction<Any?>("open_settings", "Settings", R.drawable.preferences)
        val EDIT = AACAction<Any?>("edit","Edit")
        val HOME = AACAction<Any?>("home", "Home", R.drawable.homebutton)
        fun TOGGLE(isHidden: Boolean) = if (isHidden) SHOW else HIDE
    }
}




