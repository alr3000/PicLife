package com.hyperana.kindleimagekeyboard

import android.app.TaskStackBuilder
import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.os.FileObserver
import android.util.Log
import android.preference.PreferenceManager
import android.provider.Settings
import androidx.core.view.MenuItemCompat
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.*
class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    val context = this

    // Options Menu:
    val SETTINGS_ID = 1243
    val DONE_ID = 3544
    val HELP_ID = 2657


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        try {
            super.onCreate(savedInstanceState)
           setContentView(R.layout.activity_main)


            // load default settings -- false means this will not execute twice
            PreferenceManager.setDefaultValues(this, R.xml.settings, false)

            supportFragmentManager.beginTransaction().apply {
                replace(R.id.main_content, StartingFragment(), "Starting")
                addToBackStack(null)
                commit()
            }



        }catch (e: Exception) {
            displayError("failed to create activity", e)
        }
    }

    override fun onCreateNavigateUpTaskStack(builder: TaskStackBuilder?) {
        super.onCreateNavigateUpTaskStack(builder)
    }

    //put settings access in context menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        //add settings button
        menu?.add(Menu.NONE, SETTINGS_ID, 0, R.string.button_goto_settings) // 0 = first item
        menu?.add(Menu.NONE, HELP_ID, 1, R.string.button_goto_help)
       /* menu?.add(Menu.NONE, DONE_ID, 2, R.string.abc_action_mode_done).also {
            MenuItemCompat.setShowAsAction(it, MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }*/

        return super.onCreateOptionsMenu(menu)
    }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId)  {
            SETTINGS_ID -> doClickGotoSettings()
            DONE_ID -> doClickDone()
            HELP_ID -> doClickHelp()
            else -> {
                Log.w(TAG, "unhandled context menu item: " + item?.title)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }


    fun doClickGotoSettings(v: View? = null) {
        Log.d(TAG, "doClickGotoSettings")
        try {
            val intent = Intent(context, com.hyperana.kindleimagekeyboard.SettingsActivity::class.java)
            startActivity(intent)
        }
        catch (e: Exception) {
            displayError("Could not open settings", e)
        }
    }

    fun doClickHelp(v: View? = null) {
        Log.d(TAG, "doClickHelp")
        try {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, StatusFragment())
                    .commit()
        }
        catch (e: Exception) {
            displayError("Could not open help/status", e)
        }
    }

    // same as back navigation
    fun doClickDone(v: View? = null) {
        Log.d(TAG, "doClickDone")
        try {
          onBackPressed()
        }
        catch (e: Exception) {
            displayError("Could not end activity", e)
        }
    }

    // logs full error, displays message in errorview or as final if view is not set
    fun displayError(text: String?, e:Exception?) {
        Log.e(TAG, text, e)
        val message = text ?: resources.getString(R.string.default_error_message)
        val error = findViewById(R.id.errorview) as? TextView
        if (error != null) {
            error.text = message
            error.visibility = View.VISIBLE
        }
        else {
            displayFinalError(this, message, e)
        }
    }

}
