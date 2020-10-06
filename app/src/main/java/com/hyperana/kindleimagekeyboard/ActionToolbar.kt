package com.hyperana.kindleimagekeyboard

import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPresenter
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.*
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.SetMultimap

class ActionToolbar(val toolbar: Toolbar)
    :  Toolbar.OnMenuItemClickListener, View.OnAttachStateChangeListener
{
    val TAG = "ActionToolbar"
    private var actions: Multimap<ActionListener, AACAction> = HashMultimap.create()
    private var attached: Boolean = false

    init {
         toolbar.setOnMenuItemClickListener(this)
        toolbar.addOnAttachStateChangeListener(this)
    }

    fun clear(listener: ActionListener?) {
        listener?.also { actions.removeAll(it) }
            ?: actions.clear()
    }


    fun replaceActions(listener: ActionListener, list: List<AACAction>) {
        actions.replaceValues(listener, list.toMutableList())
        update()
    }

    fun setLeftCornerAction(listener: ActionListener, action: AACAction) {
        toolbar.apply {
            setNavigationIcon(action.drawableId ?: android.R.drawable.checkbox_off_background)
            navigationContentDescription = action.displayString
            setNavigationOnClickListener { listener.handleAction(action, null) }
        }
    }

    private fun update() {
        // must do after drawing toolbar
        if (attached) {
            Log.d(TAG, "updating actions")
            actions.asMap().entries.forEach { (listener, list) ->
                updateListenerActions(listener, list)
            }
            toolbar.requestLayout()
        }
        else Log.d(TAG, "not updating because not attached")
    }

    private fun updateListenerActions(listener: ActionListener, list: Collection<AACAction>) {
        val tag = listener.getActionTag()
        toolbar.menu?.apply {
            removeGroup(tag)
            list.forEach {
                this.add(tag, it.menuId, 0, it.displayString).apply {
                    it.drawableId?.also { setIcon(it) }
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
            }
        }
    }


    override fun onViewAttachedToWindow(v: View?) {
        Log.d(TAG, "onAttached")
        attached = true
        update()
    }
    override fun onViewDetachedFromWindow(v: View?) { attached = false }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        item ?: return false

        // find listener associated with the item's group:
        return actions.keys()
            .find { it.getActionTag() == item.groupId}

                // find the AACAction associated with the item:
            ?.let { listener ->
                actions.get(listener)
                    ?.find { it.menuId == item.itemId }

                        // send to listener:
                    ?.let { listener.handleAction(it, null) }
            }
            ?: false
    }
}