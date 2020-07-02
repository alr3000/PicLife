package com.hyperana.kindleimagekeyboard

import android.app.TaskStackBuilder
import android.content.*
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import java.lang.Math.floor

class MainActivity : AppCompatActivity(), FragmentListener {

    val TAG = "MainActivity"
    val context = this

    var aacManager: AACManager? = null
    val messageViewModel: IconListModel by viewModels()
    val speaker = Speaker(App.getInstance(this.applicationContext)).also {
        lifecycle.addObserver(it)
    }

    var preferenceCheckTime = 0L


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

            // load default settings -- false means this will not execute twice
            PreferenceManager.setDefaultValues(this, R.xml.settings, false)

            FragmentChainListener(R.id.loading_fragment_view, supportFragmentManager).also { chain ->
                val startup = mutableListOf<Fragment>()
                if (getKeyboardsNotLoaded(this).isNotEmpty())
                    startup.add(LoadAssetsFragment.create(chain))
                //todo: -?- add fragment to load images for current aac keyboard/page
                chain.start(startup)
            }
            setContentView(R.layout.activity_main)

           /* messageViewModel.getText()
                .observe(this, MessageBox(findViewById<EditText>(R.id.message_text), messageViewModel ))
*/
        }catch (e: Exception) {
            displayError("failed to create activity", e)
        }
    }

    override fun onResume() {
        super.onResume()

        App.getInstance(applicationContext).preferenceChangeTime.also { change ->
            if (preferenceCheckTime < change) {
                initializeAAC()
            }
            preferenceCheckTime = change
        }
    }

    fun initializeAAC() {

        aacManager = AACManager(
            App.getInstance(applicationContext),
            overlay = findViewById<ViewGroup>(R.id.imageinput_overlay),
            //todo: -L- pager type determined by preferences: one-at-a-time or momentum scroller, etc
            pager = findViewById<SwipePagerView>(R.id.pager),
            input = InputViewController(
                inputter = messageViewModel,
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
            setPages(getProjectedPages())
            setCurrentPage( app.get("currentPageId")?.toString())
            //messageViewModel.getText().observe(this@MainActivity, messageTextObserver)
        }



    }

    fun getProjectedPages() : List<PageData> {
        val app = App.getInstance(applicationContext)
        return app.getPageList()
            .let {
                RemoveBlanksProjection("icon").project(it)
            }
            .let {
                LinkedPagesProjection(app.get("createLinks").toString()).project(it)
            }
            .let {
                val cols = app.get("columns").toString().toInt()
                val rows = (if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
                    0.5 else 2.0).let { rToC ->
                    kotlin.math.round(cols.toDouble() * rToC).toInt()
                }

                FittedGridProjection(
                    cols = cols,
                    rows = rows,
                    margins = app.get("iconMargin").toString().toIntOrNull()
                ).project(it)
            }
    }

    override fun onCreateNavigateUpTaskStack(builder: TaskStackBuilder?) {
        super.onCreateNavigateUpTaskStack(builder)
    }

    //no context menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return false
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
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
