package com.hyperana.kindleimagekeyboard

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference


/**
 * Created by alr on 9/23/17.
 *
 *
 * //todo: pop-up list preference doesn't always reflect current setting, though summary does....
 *
 * SETTINGS: createLinks, homeDirectory, customKeyboard,
 */
class SettingsFragment: PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

    val TAG = "SettingsFragment"
    val RELOAD_AFTER_CHANGE = listOf("createLinks", "customKeyboard")
    var mSharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate")


    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(TAG, "onCreateSettings")
        try {
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.settings)

            mSharedPreferences = this.preferenceManager.sharedPreferences


            // Set pref's associated fragments:
            findPreference<Preference>("addKeyboard")!!.fragment = ManageKeyboardsFragment::class.java.name



        } catch (e: Exception) {
            displayError("problem loading settings", e)
        }
    }

    override fun onResume() {
        try {
            Log.d(TAG, "onResume")
            super.onResume()

            // Set summaries to show initial choices
            mSharedPreferences?.all?.keys?.onEach {
                updateSummary(it)
            }
            // register listeners
            mSharedPreferences!!
                    .registerOnSharedPreferenceChangeListener(this)


            // set keyboard picker file list
            val keyboardList = getKeyboardsDirectory(requireActivity()).list()
            val keyboardPref = (findPreference<ListPreference>("currentKeyboard") as ListPreference)
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
            mSharedPreferences!!
                    .unregisterOnSharedPreferenceChangeListener(this)
        }catch(e: Exception) {
            displayError("pause fail", e)
        }
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
         try {
            Log.d(TAG, "sharedPreferenceChanged: " + key)

             // update summary for list preferences
             key?.also { updateSummary(it) }


        } catch (e: Exception) {
            displayError("changed preference listener error", e)
        }
    }

    fun updateSummary(key: String) {
        val value = mSharedPreferences?.all?.get(key)?.toString()
        val pref = findPreference<Preference>(key)
        pref?.summary = value ?: "--not specified--"
    }

    fun isKeyRequiresReload(key: String?) : Boolean {
        return ((key != null) && RELOAD_AFTER_CHANGE.contains(key))
    }

    fun displayError(text: String?, e: Exception?) {
        Log.e(TAG, text, e)
        displayInfo(requireActivity(), text?:resources.getString(R.string.default_error_message))
    }

}