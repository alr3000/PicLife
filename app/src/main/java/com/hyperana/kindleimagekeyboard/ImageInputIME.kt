package com.hyperana.kindleimagekeyboard

/**
 * Created by alr on 6/7/17.
 *
 *
 */
import android.content.Context
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.util.DisplayMetrics
import android.util.Log
import android.util.Printer
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import java.util.*

//todo: -?- settings could be accessed through notification instead while service is running

//todo: -L- input as emoji (image instead of text) mode
// Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this)) logs - todo
// only top (outside call method) level displays error - todo

class ImageInputIME(): InputMethodService(), LifecycleOwner {

    // Logger:
    val TAG = "ImageInputIME"

    // make service "lifecycle":
    private val lifecycleRegistry = LifecycleRegistry(this)


    // Icon Page Variables:
    var currentEditorInfo: EditorInfo? = null
    var preferenceCheckTime: Long = 0L

    // views:
    var view: ViewGroup? = null
    var iconListeners: List<IconListener> = listOf()


    val wordInputter: WordInputter = IMEWordInputter(this)


    //********************************** InputMethod Overrides: ********************************

    // check highlight action button
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        Log.d(TAG, "onStartInputView")

        // capture any initial text:
        // todo: create new icon list model with current text.
        // then update with cursor position (word index) and listen for changes
        //wordInputter.update()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        super.onStartInputView(info, restarting)
    }

    override fun onCreateInputMethodSessionInterface(): AbstractInputMethodSessionImpl {
        Log.d(TAG, "onCreateInputMethodSessionInterface")
        return super.onCreateInputMethodSessionInterface()
    }

    override fun onWindowHidden() {
        Log.d(TAG, "onWindowHidden")
        super.onWindowHidden()
    }


    override fun onFinishInput() {
        Log.d(TAG, "onFinishInput")
        super.onFinishInput()
    }

    // check preferences when returning to IME: recreates view if changed
    override fun onWindowShown() {
        Log.d(TAG, "onWindowShown")
        super.onWindowShown()
        try {
            if (App.getInstance(applicationContext).preferenceChangeTime > preferenceCheckTime) {
                Log.d(TAG, "preferences changed!")
                preferenceCheckTime = Date().time
                setInputView(createInputView())
            }
        }
        catch(e:Exception) {
            Log.e(TAG, "could not refresh on configure window", e)
        }
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        Log.d(TAG, "onCurrentInputMethodSubtypeChanged")
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "onStartInput -- restarting? " + restarting.toString())

        // get Editor Info
        Log.d(TAG, "EditorInfo: " + attribute?.fieldName + " ACTION: "+attribute?.actionLabel +
                " OPTIONS: " + attribute?.imeOptions?.and(EditorInfo.IME_MASK_ACTION))
        currentInputEditorInfo.dump(Printer { Log.d(TAG, "editorInfo: " + it) }, "")
        currentEditorInfo = attribute





    }

    override fun onUnbindInput() {
        super.onUnbindInput()
        Log.d(TAG, "onUnbindInput")
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d(TAG, "onFinishInputView: $finishingInput")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        App.getInstance(applicationContext).iconEventLiveData.removeObservers(this)
        super.onDestroy()
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        App.getInstance(applicationContext).iconEventLiveData.observe(this, object: Observer<IconEvent?> {
            init { Log.d(TAG, "observe IconEvent...")}
            override fun onChanged(t: IconEvent?) {
                Log.d(TAG, "iconEvent::onChange ${t?.icon?.text} -- ${t?.action}")
                iconListeners.forEach {
                    it.onIconEvent(t?.icon, t?.action, t?.view)
                }
            }
        })
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    override fun onTrimMemory(level: Int) {
        Log.d(TAG, "onTrimMemory: "+level)
        super.onTrimMemory(level)
    }

    override fun onLowMemory() {
        Log.d(TAG, "onLowMemory")
        super.onLowMemory()
    }

    override fun onBindInput() {
        Log.d(TAG, "onBindInput")
        super.onBindInput()
    }

    //https://stackoverflow.com/questions/19957973/inputmethodservice-oncreateinputview-never-called
    override fun onShowInputRequested(flags: Int, configChange: Boolean) : Boolean {
        Log.d(TAG, "onShowInputRequested returns true")
        return true
    }

    // keep this UI as a fullscreen overlay in landscape, if possible
    override fun  onEvaluateFullscreenMode(): Boolean {
        val out =  (super.onEvaluateFullscreenMode() ||
                (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE))
        Log.d(TAG, "onEvaluateFullScreenMode returns " + out)
        return out
    }



    //********************************** CREATE VIEW ********************************************
    //Create and return the view hierarchy used for the input area (such as a soft keyboard).
    // This will be called once, when the input area is first displayed.
    //To control when the input view is displayed, implement onEvaluateInputViewShown().
    // To change the input view after the first one is created by this function, use setInputView(View).
    // Icons are set in separate function posted to view to be run after imagepageview has been laid out
    override fun onCreateInputView(): View {
        try {
            Log.d(TAG, "onCreateInputView: is fullscreen? " + isFullscreenMode)
            return createInputView()

        } catch (e: Exception) {
            displayError("failed to create keyboard view", e)
            return View(this)
        }
    }



    // ********************************** View Helpers *************************************************
    // uses preferences:
    //      columns, createLinks, margins??
    fun createInputView() : View {
        Log.d(TAG, "createInputView")

        val layout = if (isFullscreenMode) R.layout.input_method_horiz else R.layout.input_method_main
        view = layoutInflater.inflate(layout, null) as ViewGroup

        // embedded keyboard view should not take up whole screen
        resizeView(
            view = view?.findViewById(R.id.imageinput_container) as ViewGroup,
            aspectRatio = if (isFullscreenMode) 0.8f else 0.5f
        )

        val app = App.getInstance(applicationContext)

        AccessSettingsController(
            requestSettingsView = view!!.findViewById(R.id.preferences_button),
            gotoSettingsView = view!!.findViewById(R.id.settings_button),
            overlay = view?.findViewById<ViewGroup>(R.id.imageinput_overlay)
        )

        //todo: -?- register speaker and inputview for icon localbroadcasts
        iconListeners = listOf(
            Speaker(app).also {
                lifecycle.addObserver(it)
            },
            MessageViewController(
                app = app,
                lifecycleOwner = this,
                inputter = wordInputter,
                overlay = view!!.findViewById<ViewGroup>(R.id.imageinput_overlay),
                backspaceView = view!!.findViewById(R.id.backspace_button),
                forwardDeleteView = view!!.findViewById(R.id.forwarddel_button),
                inputActionView = view!!.findViewById(R.id.done_button)
            ),
            AACManager(
                app = app,
                overlay = view?.findViewById<ViewGroup>(R.id.imageinput_overlay),
                //todo: -L- pager type determined by preferences: one-at-a-time or momentum scroller, etc
                pager = view!!.findViewById<SwipePagerView>(R.id.pager),
                gotoHomeView = view!!.findViewById(R.id.home_button),
                titleView = view!!.findViewById<TextView>(R.id.inputpage_name)
            ).apply {
                setPages(getProjectedPages())
                app.get("currentPageId")?.toString()?.also { setCurrentPage(it )}
            }
        )


        return view!!

    }

    fun getProjectedPages() : List<PageData> {
        val app = App.getInstance(applicationContext)
        return app.getPageList()
            .let {
                LinkedPagesProjection(app.get("createLinks").toString()).project(it)
            }
            .let {

                val cols = app.get("columns").toString().toInt()
                val colsToRows: List<Int> = listOf(1,1,2,2,3,3,4,5,5,6,6,7)
                val MAX_ROWS = 7

                FittedGridProjection(
                    cols = cols,
                    rows = colsToRows.getOrNull(cols) ?: MAX_ROWS,
                    margins = app.get("iconMargin").toString().toIntOrNull()
                ).project(it)
            }
    }


    fun resizeView(view: View?, aspectRatio: Float) {
        val displayMetrics = DisplayMetrics()
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.getMetrics(displayMetrics)

        view?.layoutParams?.height = (displayMetrics.heightPixels*aspectRatio).toInt()

        Log.d(
            TAG,
            "displayMetrics(" + displayMetrics.widthPixels +
                    "x" + displayMetrics.heightPixels + ")\n" +
                    "IME metrics(" + view?.layoutParams?.width.toString() +
                    "x" + view?.layoutParams?.height.toString() + ")"
        )
    }


    fun displayError(message: String, e: Exception) {
        try {
            Log.e(TAG, message, e)

        } catch (ex: Exception) {
            Log.w(TAG, "failed display error", ex)
        }
    }

    //todo: common error handling for app
    fun doClickError(v: View) {
        v.visibility = View.GONE
    }


    //********************************** Buttons ********************************************


/*
        //built-in kindle keyboard does not offer switch, so you can go to that one, but never get back
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val token = window.window.attributes.token
            if ((getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .shouldOfferSwitchingToNextInputMethod(token) == true) {
                val b = view.findViewById(R.id.switchkeyboard_button) as ImageButton
                b.visibility = View.VISIBLE
                b.setOnClickListener { v -> doClickSwitchKeyboard(v) }
            }
        }
        else {
            Log.w(TAG, "shouldOfferSwitching... returns false")
        }

*/



    /* // Switch keyboard
    fun doClickSwitchKeyboard(view: View) {
        Log.d(TAG, "doClickSwitchKeyboard")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val token = window.window.attributes.token
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)!!
                        .switchToNextInputMethod(token, false)
            }
        }
        catch(e: Exception) {
            Log.w(TAG, "Failed switch to new keyboard: " + e.message)
        }
    }
    */


    /*fun doClickClearAll(view: View): Boolean {
        Log.d(TAG, "doClickClearAll")
        // keycode.CLEAR not working for some reason
        wordInputter.clearAll(currentInputConnection)
         onChangeText(getAllText())
        return true
    }*/





    //******************************** Editor Helpers *******************************************
/*
    fun sendKey(code: Int) : Boolean {
        Log.d(TAG, "sendKey: "+code)
        return currentInputConnection.sendKeyEvent(KeyEvent(ACTION_DOWN, code)) &&
                currentInputConnection.sendKeyEvent(KeyEvent(ACTION_UP, code))
    }*/



}

