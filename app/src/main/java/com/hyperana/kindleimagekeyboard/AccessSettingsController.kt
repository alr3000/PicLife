package com.hyperana.kindleimagekeyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.util.Log
import android.view.View


// Settings access ui that shows a centered button to open settings as well
// as volume control on overlay, on OPEN_SETTINGS action. Hides both after timeout.

class AccessSettingsController(val gotoSettingsView : View) : ActionListener {
    val TAG = "AccessSettingsControllr"

    init {
        gotoSettingsView.setOnClickListener { v: View -> doClickGotoSettings(v) }
    }

    override fun handleAction(action: AACAction, data: Any?): Boolean {
        return if (action == AACAction.OPEN_SETTINGS) doClickPreferences() else false
    }

    override fun getActionTag(): Int {
        return TAG.hashCode()
    }

    fun doClickPreferences() : Boolean {
        try {
            Log.d(TAG, "doClickPreferences")
            val SETTINGS_BUTTON_TIME = 2000L

            // show volume control for music stream
            val stream = AudioManager.STREAM_MUSIC
            val am = gotoSettingsView.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(stream, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)

            // show settings link
            gotoSettingsView.visibility = View.VISIBLE
            Handler().postDelayed(
                {
                    try {
                        gotoSettingsView.visibility = View.GONE
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
            val intent = Intent(v.context, SettingsActivity::class.java)

            // if this is a service, need to start activity with its own task chain
            if (v.context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            v.context.startActivity(intent)
        }
        catch (e: Exception) {

            Log.e(TAG, "Could not open settings", e)
        }
    }
}
