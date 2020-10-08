package com.hyperana.kindleimagekeyboard

import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.preference.PreferenceManager

class MainActivity :  AppCompatActivity(), Toolbar.OnMenuItemClickListener,
    ActionListener {

    val TAG = "MainActivity"
    val context = this

    val REQUEST_STARTUP = 1

    // create here to share with message controller and tools, etc
    val messageViewModel: IconListModel by viewModels()

    // add action items here that the main activity will respond to:
    val MAIN_ACTION_TAG = 0

    lateinit var app: App
    lateinit var aacViewModel: AACViewModel

    // create actionmanager that lives within this lifecycle:
    var actionManager: ActionManager = ActionManager(lifecycle)
    var aacToolbar: ActionToolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        try {
            super.onCreate(savedInstanceState)

            app = App.getInstance(context)


            setContentView(R.layout.activity_main)
            findViewById<View>(R.id.loading_fragment_view)?.visibility = View.GONE

            aacViewModel = ViewModelProvider(context as ViewModelStoreOwner)[AACViewModel::class.java]
            aacViewModel.onRestoreInstanceState(
                savedInstanceState,
                PreferenceManager.getDefaultSharedPreferences(applicationContext)
            )

            // set message model to receive "executed" icons from anywhere in this activity:
            actionManager.registerActionListener(messageViewModel, listOf(AACAction.EXECUTE))

            // initialize speech and other activity-based action listeners:
            Speaker(app).also {
                lifecycle.addObserver(it)
                actionManager.registerActionListener(it, listOf(AACAction.SPEAK, AACAction.PREVIEW, AACAction.EXECUTE))
            }
            findViewById<ViewGroup>(R.id.imageinput_overlay)?.also {
                actionManager.registerActionListener(Highlighter(app, it), listOf(AACAction.FLASH))
            }
            AccessSettingsController(gotoSettingsView = findViewById(R.id.settings_button))
                .also { actionManager.registerActionListener(it, listOf(AACAction.OPEN_SETTINGS))}

            // wrap aac options toolbar for use with actions:
            aacToolbar = findViewById<Toolbar>(R.id.input_action_toolbar)?.let { ActionToolbar(it) }


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

    }

    // this activity consists of an aac keyboard for input, provided with
    // app for preferences and settings, global static action listener
    // main action listener for speech, sharing, etc, outputs
    // message iconListModel and message view to hold and display message state
    fun initializeAAC() {
        Log.d(TAG, "initializeAAC")
        val app = App.getInstance(applicationContext)



        MessageViewController(
            app = app,
            lifecycleOwner = this,
            iconListModel = messageViewModel,
            messageViewContainer = findViewById(R.id.message_container),
            actionManager = actionManager
        )

        AACViewController(
            app = app,
            overlay = findViewById<ViewGroup>(R.id.imageinput_overlay),
            lifecycleOwner = this,
            aacViewModel = aacViewModel,
            aacToolbar = aacToolbar,
            actionManager = actionManager
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


    // handle Action menuitems:
    override fun onMenuItemClick(item: MenuItem?): Boolean {
       return item?.itemId?.let {id -> actionManager.handleActionMenuId(id, null) }
           ?: false
    }
    override fun getActionTag(): Int {
        return MAIN_ACTION_TAG
    }

    // menu action being called from live event or other communication come with data to perform task:
    override fun handleAction(action: AACAction, data: Any?): Boolean {
        Log.i(TAG, "handleAction: $action, $data")
// doesn't handle any actions yet
        return false
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
