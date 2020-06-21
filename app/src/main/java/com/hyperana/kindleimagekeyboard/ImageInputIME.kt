package com.hyperana.kindleimagekeyboard

/**
 * Created by alr on 6/7/17.
 *
 *
 */
import android.content.res.Configuration
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.media.AudioManager.FLAG_SHOW_UI
import android.media.AudioManager.STREAM_MUSIC
import android.os.Build
import android.os.Handler
import android.preference.PreferenceManager
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_FLUSH
import android.speech.tts.TextToSpeech.SUCCESS
import android.content.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.Printer
import android.view.*
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputMethodSubtype
import android.widget.*
import java.util.*
import android.view.animation.AlphaAnimation
import android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION


//todo: -L- input as emoji (image instead of text) mode
// Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this)) logs - todo
// only top (outside call method) level displays error - todo

class ImageInputIME(): InputMethodService() , SwipePagerView.PageListener {

    // Logger:
    val TAG = "ImageInputIME"

    var app: App? = null


   // Icon Page Variables:
    var currentEditorInfo: EditorInfo? = null
    var preferenceCheckTime: Long = 0L

    // views:
    var view: ViewGroup? = null
    var overlay: ViewGroup? = null
    var keyboardView: ViewGroup? = null
    var pager: SwipePagerView? = null

    // inputter helper:
    val wordInputter = WordInputter()

    // icon interface:
    val iconListener = object: InputPageView.IconListener {
        override fun preview(icon: IconData?, v: View?) {
            Log.d(TAG, "preview icon")
            if (((icon != null) && (v != null))  && (icon.text != DEFAULT_ICON_TEXT)) {
                highlightIcon(v, icon)

                if (app!!.get("speakWords").toString() == "speakIconTouch") {
                    speakIcon(icon)
                }
            }
        }

        override fun execute(icon: IconData?, v: View?) {
            Log.d(TAG, "execute icon")
            if ((icon != null) && (icon.text != DEFAULT_ICON_TEXT)) {
                typeIcon(icon)
                if (app!!.get("speakWords").toString() == "speakIconEntry") {
                    speakIcon(icon)
                }
                gotoLinkIcon(icon)
            }
        }
    }


    // TTS:
    var TTS: TextToSpeech? = null


    //********************************** InputMethod Overrides: ********************************

    // check highlight action button
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        Log.d(TAG, "onStartInputView")

        // do things that must be done when text changed
        onChangeText(getAllText())

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
             if (app!!.preferenceChangeTime > preferenceCheckTime) {
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
        currentEditorInfo = attribute
        currentInputEditorInfo.dump(Printer { Log.d(TAG, "editorInfo: " + it) }, "")

    }



    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        // release tts
        TTS?.shutdown()
        TTS = null

        super.onDestroy()
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        try {
            app = application as App

        }
        catch(e: Exception) {
            displayError("Failed to create keyboard", e)
        }
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
        // initialize TTS:
        TTS = TTS ?: TextToSpeech(this, { status -> if (status != SUCCESS) TTS = null })
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

    override fun onPageChange(page: PageData?, index: Int) {
        Log.d(TAG, "onPageChange: " + page?.name)

        //set title
        (view?.findViewById(R.id.inputpage_name) as? TextView)?.text = page?.name ?: "Untitled"

        //set app var:
        app?.put("currentPageId", page?.id)
    }

    fun onChangeText(text: String) {
        try {
            // highlight done button if text not empty
            if (app?.get("doActionHighlight")?.toString()?.toBoolean() ?: true) {
                highlightActionButton(!text.isEmpty())
            }

            if (!text.isEmpty()) {
                //speak message if speakMessageEach
                if (app?.get("speakTextEach")?.toString()?.toBoolean() ?: false) {
                    speak(text)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "failed done button highlight", e)
        }
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
        overlay = view?.findViewById(R.id.imageinput_overlay) as ViewGroup

        // view should not take up whole screen
        resizeView(
                view = view?.findViewById(R.id.imageinput_container) as ViewGroup,
                aspectRatio = if (isFullscreenMode) 0.8f else 0.5f
        )


        //todo: -L- pager type determined by preferences: one-at-a-time or momentum scroller, etc
        pager = (view!!.findViewById(R.id.pager) as SwipePagerView)


        // build a view with iconListener for each page that has icons
        // uses columns, iconMargin, createLinks
        pager!!.adapter = object: BaseAdapter() {

            // todo: do this elsewhere as it is slow
            val pages = app!!.getPageList()
                    .let {
                        LinkedPagesProjection(app!!.get("createLinks").toString()).project(it)
                    }
                    .let {
                        FittedGridProjection(
                                cols = app!!.get("columns").toString().toInt(),
                                rows = null,
                                margins = app!!.get("iconMargin").toString().toIntOrNull()
                        ).project(it)
                    }
                    .filter { it.icons.isNotEmpty() }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                try {
                    Log.d(TAG, "pager adapter getView: " + position)

                    //todo: -L- page type determined by preference: infinite scroll, expandable, fixed
                    // reuse old pageView if available
                    // if any page setting had changed, this whole thing would be rebuilt, so they
                    // should all be reusable

                    val cv = (convertView as? InputPageView)
                    return if (cv != null) cv.refit(getItem(position) as PageData)!!
                    else InputPageView(
                            applicationContext,
                            getItem(position) as PageData,
                            app!!,
                            iconListener
                    )
                }
                catch (e: Exception) {
                    Log.e(TAG, "could not get view at " + position)
                    return View(app)
                }

            }

            override fun getItem(position: Int): Any {
                return pages.get(position)
            }

            override fun getItemId(position: Int): Long {
                return 0 // not implemented
            }

            override fun getCount(): Int {
                return pages.count()
            }
        }
        pager!!.pageListener = this

        app?.get("currentPageId")?.toString().also {
            Log.d(TAG, "currentPageId = " + it)
            if (it != null) {
                pager!!.setPageById(it)
            }
            else {
                pager!!.setPage(0) // first in list by default
            }
        }


        // set UI handlers
        setViewListeners(view!!)

        view?.findViewById<View>(R.id.home_button)?.visibility =
                if (app?.get("doHomeButton")?.toString()?.toBoolean() ?: true) View.VISIBLE
                else View.INVISIBLE


        return view!!

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
                "IME metrics(" + keyboardView?.layoutParams?.width.toString() +
                "x" + keyboardView?.layoutParams?.height.toString() + ")"
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

    fun setViewListeners(view: View) {

        //todo: -?- settings could be accessed through notification instead while service is running
        (view.findViewById(R.id.preferences_button) as ImageButton)
                .setOnClickListener { v -> doClickPreferences(v) }

        (view.findViewById(R.id.settings_button) as Button)
                .setOnClickListener { v -> doClickGotoSettings(v) }

        (view.findViewById(R.id.home_button) as ImageButton)
                .setOnClickListener { v -> doClickHome(v) }

        (view.findViewById(R.id.backspace_button) as ImageButton)
                .setOnClickListener { v -> doClickBackspace(v) }

        (view.findViewById(R.id.forwarddel_button) as ImageButton)
                .setOnClickListener { v -> doClickForwardDel(v) }

//        (view.findViewById(R.id.clearall_button) as ImageButton)
//                .setOnClickListener { v -> doClickClearAll(v) }

        (view.findViewById(R.id.done_button) as ImageButton)
                .setOnClickListener { v -> doClickDone(v) }


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
    }



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

    // delete by word or punctuation
    fun doClickBackspace(view: View): Boolean {
        Log.d(TAG, "doClickBackspace")
        try {
           wordInputter.backwardDelete(currentInputConnection)
            onChangeText(getAllText())
        }
        catch(e: Exception) {
            Log.w(TAG, "doClickBackspace problem:" + e.message)
        }
        return true
    }

    /*fun doClickClearAll(view: View): Boolean {
        Log.d(TAG, "doClickClearAll")
        // keycode.CLEAR not working for some reason
        wordInputter.clearAll(currentInputConnection)
         onChangeText(getAllText())
        return true
    }*/

    fun doClickForwardDel(view: View) : Boolean {
        val ic = currentInputConnection
        wordInputter.forwardDelete(ic)
        onChangeText(getAllText())
        return true
    }

    fun doClickDone(view: View): Boolean {
        Log.d(TAG, "call performEditorAction "+currentEditorInfo?.actionId+" on DONE")

        val text = getAllText()
        speak(text)

        currentEditorInfo?.imeOptions?.and(IME_FLAG_NO_ENTER_ACTION)
        currentInputConnection.performEditorAction(currentEditorInfo?.actionId ?: 0)
        return true
    }

    fun doClickHome(view: View): Boolean {
        Log.d(TAG, "doClickHome")
        gotoPage(0)
        return true
    }


    fun doClickPreferences(v: View) : Boolean {
        try {
            Log.d(TAG, "doClickPreferences")
            val SETTINGS_BUTTON_TIME = 2000L

            // show volume control for music stream
            val stream = STREAM_MUSIC
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(stream, AudioManager.ADJUST_SAME, FLAG_SHOW_UI)

            // show settings link
            val button = view?.findViewById(R.id.settings_button) as Button
            button.visibility = View.VISIBLE
            Handler().postDelayed(
                    {
                        try {
                            button.visibility = View.GONE
                        } catch (e: Exception) {
                            Log.e(TAG, "settings button timer code problem: ", e)
                        }
                    },
                    SETTINGS_BUTTON_TIME
            )
        }
        catch (e: Exception) {
            Log.e(TAG, "click preferences problem: ", e)
        }
        finally {
            return true
        }
    }

    fun doClickGotoSettings(v: View) {
        Log.d(TAG, "doClickGotoSettings")
        try {
            val intent = Intent(this, SettingsActivity::class.java)

            // since this is a service, need to start activity with its own task chain
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        catch (e: Exception) {

            displayError("Could not open settings", e)
        }
    }
  //************************************* ICON HANDLERS ***************************************

    fun gotoLinkIcon(icon: IconData) {
        if (icon.linkToPageId == null) {
            return
        }
        val newPageIndex = (application as App).getPageList().indexOfFirst { it.id == icon.linkToPageId }
        if (newPageIndex < 0) {
            Log.w(TAG, "linked page not found for " + icon.text + "->" + icon.linkToPageId)
            return
        }
       gotoPage(newPageIndex)
    }

    fun gotoPage(index: Int) {
        try {
            pager?.setPage(index)
        }catch (e: Exception) {
            Log.e(TAG, "go to page fail", e)
        }
    }

    //Put text into input on text icon touch
    fun typeIcon(icon: IconData) {
        Log.d(TAG, "typeIcon: " + icon.text)
        try {
            val typeLinks = app?.get("doTypeLinks")?.toString()?.toBoolean() ?: false
            if ((icon.linkToPageId != null) && (!typeLinks)) {
                return
            }
            if (icon.text == null) {
                return
            }
            wordInputter.input(currentInputConnection, icon.text!!)
            onChangeText(getAllText())
        } catch(e: Exception) {
            Log.e(TAG, "problem with icon input", e)
        }
    }

    fun highlightIcon(iconView: View, icon: IconData) {
        // use textview if path is null - todo -L-
        val img =
                if (icon.path != null) {
                    ImageView(this).also {
                        app!!.asyncSetImageBitmap(it, icon.path!!)
                    }
                }
                else null

        val highlight = HighlightView(iconView, img, app!!)
        overlay?.addView(highlight)
        Handler().postDelayed(
                {
                    try {
                        (highlight.parent as? ViewGroup)?.removeView(highlight)
                    }
                    catch (e: Exception) {
                        Log.e(TAG, "highlight timer code problem: ", e)
                    }
                },
                (app?.get("highlightTime")?.toString()?.toLongOrNull() ?: 1000)
        )
    }



    // if it's not a link, or if set to speak links, speak icon text
    fun speakIcon(icon:IconData) {
        if ((icon.linkToPageId == null) ||
                (app?.get("speakLinks")?.toString()?.toBoolean() ?: false)
                ) {
            speak(icon.text ?: "")
        }
    }

    fun speak(text: String) {
        if (app?.get("doSpeak")?.toString()?.toBoolean() ?: false) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TTS?.speak(text, QUEUE_FLUSH, null, UUID.randomUUID().toString())
            } else {
                TTS?.speak(text, QUEUE_FLUSH, null)
            }
        }
    }

    //******************************** Editor Helpers *******************************************
/*
    fun sendKey(code: Int) : Boolean {
        Log.d(TAG, "sendKey: "+code)
        return currentInputConnection.sendKeyEvent(KeyEvent(ACTION_DOWN, code)) &&
                currentInputConnection.sendKeyEvent(KeyEvent(ACTION_UP, code))
    }*/

    fun getAllText() : String {
        return (currentInputConnection?.getTextBeforeCursor(9999, 0)?.trim() ?: "").toString()
                .plus(currentInputConnection?.getTextAfterCursor(9999, 0)?.trim() ?: "")
    }



    fun highlightActionButton(start: Boolean) {
        val VIEW_TAG = "actionHighlight"
        val v = overlay?.findViewWithTag(VIEW_TAG) as? View
        Log.d(TAG, "highlightActionButton: " + start + " v=" + v.toString())

        if (start && (v == null)) {
            val highlight = HighlightView((view?.findViewById(R.id.done_button) as View), null, app!!)
            highlight.tag = VIEW_TAG
            overlay?.addView(highlight)

            val fadeIn = AlphaAnimation(0.0f, 1.0f)
            val fadeOut = AlphaAnimation(1.0f, 0.0f)
            fadeIn.duration = 500
            fadeOut.duration = 600
            fadeOut.startOffset = 600 + fadeIn.startOffset + 600
            fadeIn.repeatCount = Animation.INFINITE
            fadeOut.repeatCount = Animation.INFINITE

            highlight.startAnimation(fadeIn)
            highlight.startAnimation(fadeOut)
        }
        else if (!start && (v != null)) {
            v.clearAnimation()
            (v.parent as? ViewGroup)?.removeView(v)
            assert(
                    (overlay?.findViewWithTag<View>(VIEW_TAG) == null),
                    {"highlight view remaining. " + v.toString()}
            )
        }

    }

}

