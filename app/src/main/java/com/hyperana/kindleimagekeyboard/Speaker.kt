package com.hyperana.kindleimagekeyboard

import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import java.util.*

class Speaker(val app: App): LifecycleObserver, ActionListener {

    val TAG = "Speaker"
    var TTS: TextToSpeech? = null
    var prefs = PreferenceManager.getDefaultSharedPreferences(app.appContext)
    val doSpeak = app.appContext.resources.getString(R.string.key_speech_enabled)
    val speakTextEach = app.appContext.resources.getString(R.string.entry_speak_message_each)
    val speakTextAction = app.appContext.resources.getString(R.string.entry_speak_message_action)
    val speakIconTouch = app.appContext.resources.getString(R.string.entry_speak_touch)
    val speakIconEnter = app.appContext.resources.getString(R.string.entry_speak_enter)
    val speakLinks = app.appContext.resources.getString(R.string.key_speak_links)
    val speakWords = app.appContext.resources.getString(R.string.key_speak_icons)
    val speakText = app.appContext.resources.getString(R.string.key_speak_text)

    fun speak(text: String) {
        Log.d(TAG, "speak [$text]")

        if (!prefs.getBoolean(doSpeak, false)) return

        fun doSpeak(text: String) {
            TTS?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }

        // initialize TTS:
        TTS = TTS?.also { doSpeak(text) }
            ?: TextToSpeech(app.appContext) { status ->
                Log.i(TAG, "TTS initialized: ${status == TextToSpeech.SUCCESS}")
                if (status != TextToSpeech.SUCCESS) TTS = null
                else doSpeak(text)
            }

    }

    // if it's not a link, or if set to speak links, speak icon text
    fun speakIcon(icon:IconData) {
        val speakLinks = prefs.getBoolean(speakLinks, false)

        if ((icon.linkToPageId == null) || speakLinks ) {
            (icon.text?.let { if (it == DEFAULT_ICON_TEXT) "beep" else it } ?: "")
                .also { speak(it) }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun startTTS() {
        Log.d(TAG, "init")
        speak("")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stopTTS() {
        Log.d(TAG, "TTS shutdown")
        TTS?.shutdown()
        TTS = null
    }



    fun execute(icon: IconData?, v: View?) {
        if (icon != null
            && prefs.getString(speakWords, "") == speakIconEnter )

            speakIcon(icon)

    }

    fun preview(icon: IconData?, v: View?) {
        if (icon != null
            && prefs.getString(speakWords, "").also { Log.d(TAG, "speakWords: $it")} == speakIconTouch )

            speakIcon(icon)
    }

    // ActionInterface:
    override fun handleAction(action: AACAction, data: Any?): Boolean {
        Log.d(TAG, "handleAction: $action, $data")
        when (action) {
            AACAction.EXECUTE -> (data as? List<*>)
                ?.forEach { execute(it as? IconData, null) }
            AACAction.PREVIEW -> (data as? List<*>)
                ?.forEach { preview(it as? IconData, null) }
            AACAction.SPEAK -> speak(data?.toString() ?: "")
        }
        return false
    }

    override fun getActionTag(): Int {
        return toString().hashCode()
    }

    // IconList interface:

    val messageObserver = object: Observer<List<IconData>> {
        //todo: speaker observes iconlistmodel

        override fun onChanged(t: List<IconData>?) {
            if (t?.isNotEmpty() == true)
            {
                //speak message if speakMessageEach
                if (prefs.getString(speakText, "") == speakTextEach )

                    speak(t.joinToString(" "))
            }
        }
    }
}