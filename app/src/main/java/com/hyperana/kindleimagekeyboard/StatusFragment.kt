package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.fragment.app.Fragment
import java.io.File

/**
 * Created by alr on 11/10/17.
 */
class StatusFragment: Fragment(), ViewTreeObserver.OnGlobalLayoutListener {
    val TAG = "StatusFragment"

    var enabledView: StatusTextView? = null
    var loadedView: StatusTextView? = null
    var chosenView: StatusTextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        try {
            super.onCreate(savedInstanceState)

            // load default settings -- false means this will not execute twice
            PreferenceManager.setDefaultValues(activity, R.xml.settings, false)
        }
        catch (e: Exception) {
            Log.e(TAG, "failed create", e)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            val view = inflater.inflate(R.layout.fragment_status_content, container!!, false)

            // views:
            enabledView = StatusTextView(
                    context = activity!!,
                    okTextId = R.string.status_keyboard_enabled,
                    nokTextId = R.string.status_keyboard_disabled,
                    helpTextId = R.string.enable_keyboard_help
            )
            loadedView = StatusTextView(
                    context = activity!!,
                    okTextId = R.string.status_keyboard_loaded,
                    nokTextId = R.string.status_keyboard_not_found,
                    helpTextId = R.string.load_keyboard_help
            )
            chosenView = StatusTextView(
                    context = activity!!,
                    okTextId = R.string.status_keyboard_chosen,
                    nokTextId = R.string.status_keyboard_notchosen,
                    helpTextId = R.string.choose_keyboard_help
            )

            val infoBox = view.findViewById(R.id.info_container) as ViewGroup
                loadedView!!.setOk(checkIsLoaded()).createIn(infoBox)
                enabledView!!.setOk(checkIsEnabled()).createIn(infoBox)
                enabledView!!.addActionButton(createDeviceSettingsButton())

                chosenView!!.setOk(checkIsChosen()).createIn(infoBox)

            return view
        }
        catch (e: Exception) {
            Log.e(TAG, "failed to create activity", e)
            //displayError("failed to create activity", e)
            return View(activity)
        }
    }

    // check if current IME has changed
    override fun onGlobalLayout() {
            Log.d(TAG, "onGlobalLayout, check isChosen")
            chosenView!!.setOk(checkIsChosen())
    }

    override fun onPause() {
        super.onPause()

        try {
            view!!.findViewById<View>(R.id.content)
                .viewTreeObserver
                .removeOnGlobalLayoutListener(this)
        }
        catch (e: Exception) {
            Log.w(TAG, "layout listener not attached for check in-use")
        }
    }

    override fun onResume() {
        try {
            super.onResume()

            //todo: not called on change keyboard
            view?.findViewById<View>(R.id.content)
                    ?.viewTreeObserver
                    ?.addOnGlobalLayoutListener(this)

            enabledView?.setOk(checkIsEnabled())
            loadedView?.setOk(checkIsLoaded())
        }
        catch (e: Exception) {
            Log.e(TAG, "could not resume", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun doClickError(v: View) {
        v.visibility = View.GONE
    }

    //***************************** STATUS CHECKERS **********************************************
    fun checkIsEnabled() : Boolean {
        return ((activity?.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.enabledInputMethodList
                ?.find { it.packageName == activity?.packageName} != null)
    }

     fun checkIsLoaded() : Boolean {
        val name = (activity?.application as? App)
                ?.get("currentKeyboard")
                ?.toString()
        if (name == null) {
            throw Exception("No current keyboard set!")
        }
        Log.d(TAG, "checkIsLoaded: " + name)
        return activity
            ?.let { getKeyboardsDirectory(it) }
            ?.let { it.exists() && it.list()?.contains(name) == true}
            ?: false
    }

    fun checkIsChosen() : Boolean {
        // check textedit connection
        val currentKeyboardId =  Settings.Secure.getString(
                activity?.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
        )
        return ((activity?.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.enabledInputMethodList
                ?.indexOfFirst {
                    ((it.id == currentKeyboardId) && (it.packageName == activity?.packageName))
                } != -1)
    }

    //****************************** VIEW HANDLERS ***********************************************

    fun createDeviceSettingsButton() : Button {
        val b = Button(activity)
        b.setText(R.string.button_goto_device_settings)
        b.setOnClickListener{
            try {
                startActivityForResult(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS), 0)
            } catch (e: Exception) {
                Log.e(TAG, "could not open device settings")
                //displayError("could not open device settings", e)
            }
        }
        return b
    }

    fun createManageKeyboardsButton() : Button {
        val b = Button(activity)
        b.setText(R.string.button_manage_keyboards)
        b.setOnClickListener{
            try {
                fragmentManager!!.beginTransaction()
                        .replace(R.id.main_content, ManageKeyboardsFragment(), "Keyboards")
                        .addToBackStack(null)
                        .commit()
            } catch (e: Exception) {
                Log.e(TAG, "could not open manage keyboards")
            }
        }
        return b
    }

}