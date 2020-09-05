package com.hyperana.kindleimagekeyboard

import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Observer
import java.util.*

class Speaker(val app: App): LifecycleObserver, IconListener {

    var TTS: TextToSpeech? = null


    fun speak(text: String) {

        fun doSpeak(text: String) {
            if (app.get("doSpeak")?.toString()?.toBoolean() ?: false) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    TTS?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
                } else {
                    TTS?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
                }
            }
        }

        // initialize TTS:
        TTS = TTS?.also { doSpeak(text) }
            ?: TextToSpeech(app.appContext) { status ->
                if (status != TextToSpeech.SUCCESS) TTS = null
                else doSpeak(text)
            }

    }

    // if it's not a link, or if set to speak links, speak icon text
    fun speakIcon(icon:IconData) {
        if ((icon.linkToPageId == null) ||
            (app.get("speakLinks")?.toString()?.toBoolean() ?: false)
        ) {
            speak(icon.text ?: "")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun startTTS() {
        speak("")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stopTTS() {
        TTS?.shutdown()
    }

    // Icon interface:
    override fun onIconEvent(icon: IconData?, action: AACAction?, view: View?) {
        when (action) {
            ICON_EXECUTE -> execute(icon, view)
            ICON_PREVIEW -> preview(icon, view)
        }
    }

    fun execute(icon: IconData?, v: View?) {
        if (icon != null && app.get("speakWords").toString() == "speakIconEntry") {
            speakIcon(icon)
        }
    }

    fun preview(icon: IconData?, v: View?) {
        if (icon != null && app.get("speakWords").toString() == "speakIconTouch") {
            speakIcon(icon)
        }
    }

    // IconList interface:

    val messageObserver = object: Observer<List<IconData>> {
        //todo: speaker observes iconlistmodel

        override fun onChanged(t: List<IconData>?) {
            if (t?.isNotEmpty() == true)
            {
                //speak message if speakMessageEach
                if (app.get("speakTextEach")?.toString()?.toBoolean() ?: false) {
                    speak(t.joinToString(" "))
                }
            }
        }
    }
}