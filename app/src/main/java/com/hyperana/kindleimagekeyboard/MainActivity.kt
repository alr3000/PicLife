package com.hyperana.kindleimagekeyboard

import android.app.TaskStackBuilder
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity(), FragmentListener {

    val TAG = "MainActivity"
    val context = this

    val messageViewModel: IconListModel by viewModels()

   var iconListeners: List<IconListener> = listOf()

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

            // attach viewcontrollers to livedata here because this is the relevant lifecycle
            // todo: remove view reference from livedata?
            App.getInstance(applicationContext)
                .iconEventLiveData.observe(this@MainActivity, object: Observer<IconEvent?> {
                override fun onChanged(t: IconEvent?) {
                    iconListeners.forEach {
                        it.onIconEvent(t?.icon, t?.action, t?.view)
                    }
                }
            })


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

    override fun onDestroy() {
        super.onDestroy()

        App.getInstance(applicationContext).iconEventLiveData.removeObservers(this)
    }

    fun initializeAAC() {
        Log.d(TAG, "initializeAAC")
        val app = App.getInstance(applicationContext)

        iconListeners = listOf(
            Speaker(App.getInstance(this.applicationContext)).also {
                lifecycle.addObserver(it)
            },
            MessageViewController(
                app = app,
                lifecycleOwner = this,
                inputter = messageViewModel,
                overlay = findViewById<ViewGroup>(R.id.imageinput_overlay),
                backspaceView = findViewById(R.id.backspace_button),
                forwardDeleteView = findViewById(R.id.forwarddel_button),
                inputActionView = findViewById(R.id.done_button)
            ),
            AACManager(
                app = app,
                overlay = findViewById<ViewGroup>(R.id.imageinput_overlay),
                pager = findViewById<SwipePagerView>(R.id.pager),
                gotoHomeView = findViewById(R.id.home_button),
                titleView = findViewById<TextView>(R.id.inputpage_name)
            ).apply {
                setPages(getProjectedPages())
                setCurrentPage( app.get("currentPageId")?.toString())
            }
        )

        AccessSettingsController(
            requestSettingsView = findViewById(R.id.preferences_button),
            gotoSettingsView = findViewById(R.id.settings_button),
            overlay = findViewById<ViewGroup>(R.id.imageinput_overlay)
        )

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
