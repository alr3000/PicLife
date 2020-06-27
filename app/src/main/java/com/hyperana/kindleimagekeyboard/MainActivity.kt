package com.hyperana.kindleimagekeyboard

import android.app.TaskStackBuilder
import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity(), FragmentListener {

    val TAG = "MainActivity"
    val context = this

    // Options Menu:
    val SETTINGS_ID = Menu.FIRST + 1
    val HELP_ID = Menu.FIRST + 2


    var aacManager: AACManager? = null
    val wordInputter: WordInputter? = null


    override fun closeFragment(fragment: Fragment) {
       removeFragment(fragment)
    }

    fun removeFragment(fragment: Fragment) {
        Log.d(TAG, "remove fragment: $fragment")
        if (supportFragmentManager.fragments.contains(fragment))
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
    }

    class FragmentChainListener (
        val containerId: Int,
        val manager: FragmentManager)
        : FragmentListener {

        private var fragments = mutableListOf<Fragment>()

        fun start(fragmentList: List<Fragment>) {
            fragments = fragmentList.toMutableList()
        }

        private fun nextFragment() : Boolean{
            if (fragments.isNotEmpty())
                fragments.removeAt(0).also {
                    manager.beginTransaction()
                        .replace(containerId, it)
                        .commit()
                    return true
                }
            else return false
        }

        override fun closeFragment(fragment: Fragment) {
            try {
                if (!nextFragment()) {
                    manager.beginTransaction()
                        .remove(fragment)
                        .commit()
                }
            }
            catch (e: Exception) {
                Log.e("FragmentChainListener", "failed close chain fragment", e)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // load default settings -- false means this will not execute twice
            PreferenceManager.setDefaultValues(this, R.xml.settings, false)

            FragmentChainListener(R.id.loading_fragment_view, supportFragmentManager).also { chain ->
                val startup = mutableListOf<Fragment>()
                if (getKeyboardsNotLoaded(this).isNotEmpty())
                    startup.add(LoadAssetsFragment.create(chain))
                //todo: add fragment to load images for current aac keyboard/page
                chain.start(startup)
            }

            aacManager = AACManager(
                App.getInstance(applicationContext),
                overlay = findViewById<ViewGroup>(R.id.imageinput_overlay),
                //todo: -L- pager type determined by preferences: one-at-a-time or momentum scroller, etc
                pager = findViewById<SwipePagerView>(R.id.pager),
                input = InputViewController(
                    inputter = wordInputter,
                    backspaceView = findViewById(R.id.backspace_button),
                    forwardDeleteView = findViewById(R.id.forwarddel_button),
                    inputActionView = findViewById(R.id.done_button)
                ),
                accessSettings = AccessSettingsController(
                    requestSettingsView = findViewById(R.id.preferences_button),
                    gotoSettingsView = findViewById(R.id.settings_button),
                    overlay = findViewById<ViewGroup>(R.id.imageinput_overlay)
                ),
                gotoHomeView = findViewById(R.id.home_button),
                titleView = findViewById<TextView>(R.id.inputpage_name)

                //todo: -?- settings could be accessed through notification instead while service is running
            ).apply {
                setPages(app.getProjectedPages())
                setCurrentPage( app.get("currentPageId")?.toString())
            }.also { wordInputter?.textListener = it }



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

        return super.onCreateOptionsMenu(menu)
    }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId)  {
            SETTINGS_ID -> doClickGotoSettings()
            HELP_ID -> doClickHelp()
            else -> {
                Log.w(TAG, "unhandled context menu item: " + item?.title)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
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
                    .add(R.id.loading_fragment_view, StatusFragment(), "keyboardStatus")
                    .addToBackStack(null)
                    .commit()
        }
        catch (e: Exception) {
            displayError("Could not open help/status", e)
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
