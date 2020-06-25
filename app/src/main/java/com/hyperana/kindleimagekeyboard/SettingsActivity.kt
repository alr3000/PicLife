package com.hyperana.kindleimagekeyboard

import android.content.SharedPreferences
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat


/**
 * Created by alr on 9/23/17.
 *
 *
 * //todo: pop-up list preference doesn't always reflect current setting, though summary does....
 * todo: import directories from drive???
 *
 * SETTINGS: createLinks, homeDirectory, customKeyboard,
 */
class SettingsActivity : FragmentActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    val TAG = "SettingsActivity"


    // Fragment Listeners ---
    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference?
    ): Boolean {
        try {
            Log.d(TAG, "onPreferenceStartFragment: ${pref?.key} -> ${pref?.fragment}")

            pref?.fragment
                ?.let { Class.forName(it).newInstance() as? Fragment }
                ?.also {frag ->

                    // Display the fragment as the main content.
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, frag)
                        .addToBackStack(null)
                        .commit()
                    return true
                }
        } catch (e: Exception) {
            displayError("could not open managekeyboards fragment", e)
            return true
        }

        return false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_settings)

        supportFragmentManager.beginTransaction()
            .replace(R.id.content, SettingsFragment(), "mainSettings")
            .commit()


    }

    fun displayError(text: String?, e: Exception?) {
        Log.e(TAG, text, e)
        displayInfo(this, text?:resources.getString(R.string.default_error_message))
    }

}