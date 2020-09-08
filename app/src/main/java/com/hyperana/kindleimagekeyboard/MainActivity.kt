package com.hyperana.kindleimagekeyboard

import android.app.TaskStackBuilder
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity :  AppCompatActivity() {

    val TAG = "MainActivity"
    val context = this

    val REQUEST_STARTUP = 1

    // create here to share with message controller and tools, etc
    val messageViewModel: IconListModel by viewModels()

    lateinit var aacViewModel: AACViewModel


    var iconListeners: List<IconListener> = listOf()

    var preferenceCheckTime = 0L



    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        try {
            super.onCreate(savedInstanceState)

            // load default settings -- false means this will not execute twice
            PreferenceManager.setDefaultValues(this, R.xml.settings, false)

            aacViewModel = ViewModelProvider(context as ViewModelStoreOwner)[AACViewModel::class.java]
            aacViewModel.onRestoreInstanceState(
                savedInstanceState,
                PreferenceManager.getDefaultSharedPreferences(applicationContext)
            )


            setContentView(R.layout.activity_main)


            // attach viewcontrollers to livedata here because this is the relevant lifecycle
            // todo: remove view reference from livedata?
            App.getInstance(applicationContext)
                .iconEventLiveData.observe(this@MainActivity, object: Observer<IconEvent?> {
                override fun onChanged(t: IconEvent?) {
                    Log.d(TAG, "iconEvent: $t")
                    iconListeners.forEach {
                        it.onIconEvent(t?.icon, t?.action, t?.view)
                    }
                }
            })

      //      startActivityForResult(Intent(this, LaunchActivity::class.java), REQUEST_STARTUP)

        }catch (e: Exception) {
            displayError("failed to create activity", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_STARTUP) {
            Log.i(TAG, "finished startup")
        }
    }

    override fun onResume() {
        super.onResume()

        initializeAAC()

       /* App.getInstance(applicationContext).preferenceChangeTime.also { change ->
            if (preferenceCheckTime < change) {
                initializeAAC()
            }
            preferenceCheckTime = change
        }*/
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        aacViewModel.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState, outPersistentState)
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
                inputActionView = findViewById(R.id.done_button),
                actions = listOf(MESSAGE_CLEAR, MESSAGE_SPEAK),
                messageViewContainer = findViewById(R.id.message_container)
            ),
            AACManager(
                app = app,
                overlay = findViewById<ViewGroup>(R.id.imageinput_overlay),
                aacViewModel = aacViewModel,
                gotoHomeView = findViewById(R.id.home_button),
                titleView = findViewById<TextView>(R.id.inputpage_name)
            )
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
