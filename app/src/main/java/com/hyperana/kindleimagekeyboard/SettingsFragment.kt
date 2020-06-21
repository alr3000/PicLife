package com.hyperana.kindleimagekeyboard

import android.app.Service
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService


/**
 * Created by alr on 9/23/17.
 *
 *
 * //todo: pop-up list preference doesn't always reflect current setting, though summary does....
 *
 * SETTINGS: createLinks, homeDirectory, customKeyboard,
 */
class SettingsFragment: PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

    val TAG = "SettingsFragment"
    val reloadingChanges = listOf("createLinks", "customKeyboard")
    var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate")

        try {
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.settings)

            // Set summaries to show initial choices
            sharedPreferences = this.preferenceManager.sharedPreferences


            // Set click listener for directory picker
             findPreference("addKeyboard").setOnPreferenceClickListener( {
                preference ->

                try {
                    Log.d(TAG, "onPreferenceClick: keyboardDataFile")
                    // Display the fragment as the main content.
                     /* fragmentManager.beginTransaction()
                            .replace(android.R.id.content, ManageKeyboardsFragment())
                            .addToBackStack(null)
                            .commit()*/
                    true

                } catch (e: Exception) {
                    displayError("could not open managekeyboards fragment", e)
                    true
                }
            })

       } catch (e: Exception) {
            displayError("problem loading settings", e)
        }
    }

    override fun onResume() {
        try {
            Log.d(TAG, "onResume")
            super.onResume()

            sharedPreferences?.all?.keys?.onEach {
                updateSummary(it)
            }
            // register listeners
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this)


            // set keyboard picker file list
            val keyboardList = getKeyboardsDirectory(activity).list()
            val keyboardPref = (findPreference("currentKeyboard") as ListPreference)
            keyboardPref.entries = keyboardList
            keyboardPref.entryValues = keyboardList


        } catch (e:Exception) {
            displayError("problem resuming preference fragment", e)
        }
    }

    override fun onPause() {
        try {
            Log.d(TAG, "onPause")
            super.onPause()
            preferenceScreen.sharedPreferences
                    .unregisterOnSharedPreferenceChangeListener(this)
        }catch(e: Exception) {
            displayError("pause fail", e)
        }
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
         try {
            Log.d(TAG, "sharedPreferenceChanged: " + key)

             // update summary for list preferences
             updateSummary(key)


        } catch (e: Exception) {
            displayError("changed preference listener error", e)
        }
    }

    fun updateSummary(key: String?) {
        val value = sharedPreferences?.all?.get(key)?.toString()
        val pref = findPreference(key)
        pref?.summary = value ?: "--not specified--"
    }

    fun isKeyRequiresReload(key: String?) : Boolean {
        return ((key != null) && reloadingChanges.contains(key))
    }

    fun displayError(text: String?, e: Exception?) {
        Log.e(TAG, text, e)
        displayInfo(activity, text?:resources.getString(R.string.default_error_message))
    }

}