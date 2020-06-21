package com.hyperana.kindleimagekeyboard

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.os.FileObserver
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Adapter.NO_SELECTION
import androidx.fragment.app.ListFragment
import java.io.File

/**
 * Created by alr on 11/5/17.
 *
 * Presents a list of keyboard data files stored in app data
 * Add (load/parse) a new keyboard data file
 * Select a keyboard file to use, save it to "keyboardDataFile" preference
 * Standard keyboard is not listed, but saved as preference if current is deleted
 *
 * todo: -L- share keyboard buttons zips directory
 * todo: -L- enable and disable keyboards (show on listpreference), lock manage keyboards page
 *
 */
class ManageKeyboardsFragment : ListFragment() {

    val TAG = "ManageKeyboardsFragment"
    var dataFilesObserver: FileObserver? = null
    var currentKeyboard: String? = null
    var defaultKeyboard: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {

            Log.d(TAG, "onCreateView")
            val view = inflater!!.inflate(R.layout.fragment_manage_directories, container!!, false)

            // watch for delete events. create events happen while observer is paused.
            initializeFileObserver()

            // set add button listener -> CreateKeyboardActivity:
            view.findViewById<Button>(R.id.button_add_directory).setOnClickListener {
                try {
                    Log.d(TAG, "onClick add directory")
                    startActivity(Intent(
                            activity!!.applicationContext,
                            com.hyperana.kindleimagekeyboard.CreateKeyboardActivity::class.java
                    ))
                }
                catch (e: Exception) {
                    Log.e(TAG, "could not show choose directory dialog", e)
                }
            }

            return view
        }
        catch (e: Exception) {
            Log.e(TAG, "could not build fragment view", e)
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }


    // on click item, save as preference, exit
    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        Log.d(TAG, "onItemClick:" + position)
        super.onListItemClick(l, v, position, id)
        try {
            val name = (listAdapter!!.getItem(position) as Map<String, String>)["name"].toString()
            saveKeyboardPreference(name)
            //fragmentManager.popBackStack()
        } catch (e: Exception) {
            Log.e(TAG, "select path failed", e)
        }
    }

    override fun onResume() {
        super.onResume()
        setListViewData()
        dataFilesObserver?.startWatching()
    }

    override fun onPause() {
        super.onPause()
        dataFilesObserver?.stopWatching() //sends ignore event to fileobserver
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    //************************* MANAGE DATA FILE LIST *********************************************
    fun initializeFileObserver() {
        activity?.also { a ->
            dataFilesObserver = object : FileObserver(
                File(a.getDir (APP_KEYBOARD_PATH, Context.MODE_PRIVATE).absolutePath)
            ) {
            override fun onEvent(event: Int, path: String?) {
                Log.d(TAG, "dataFileObserver onEvent: " + event + " on " + path)

                // mask event to exclude special directory, etc, bits
                if ((event and FileObserver.CREATE == FileObserver.CREATE)
                    || (event and FileObserver.DELETE == FileObserver.DELETE)
                ) {

                    // fileObserver is in a different thread
                    a.runOnUiThread {
                        setListViewData()
                    }
                }
            }
        }
        }
    }

    fun setListViewData() {
        Log.d(TAG, "setListViewData")
        activity?.also { a ->
            defaultKeyboard = a.resources.getString(R.string.default_keyboard_name)

            val data = a.getDir(APP_KEYBOARD_PATH, Context.MODE_PRIVATE).listFiles()
                .filter { ((it.isDirectory) && (it.name != defaultKeyboard!!)) }
                .map {
                    mapOf(
                        Pair("path", it.path),
                        Pair("name", it.name),
                        Pair("date", it.lastModified())
                    )
                }
                .sortedByDescending {
                    (it["date"] as? Long) ?: 0
                }

            listAdapter = object : SimpleAdapter(
                a,
                data,
                R.layout.directory_item, // item view
                arrayOf("name"), // item map key
                intArrayOf(R.id.directory_item_path) // view in item view
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    var view = convertView // or super.getView?
                    if (view == null) {
                        val inflater = a.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                                as LayoutInflater
                        view = inflater.inflate(R.layout.directory_item, null)
                    }

                    val item = (this.getItem(position) as Map<String, String>)

                    //Handle TextView and display string from your list
                    val listItemText = view?.findViewById(R.id.directory_item_path) as? TextView
                    listItemText?.setText(item.get("name"))

                    //Can't delete default keyboard
                    view?.findViewById<ImageButton>(R.id.directory_item_delete)?.apply {
                        setOnClickListener {
                            doClickDeleteKeyboard(item["name"]!!)
                        }
                    }

                    return view!!

                }
            }

            // set current selection
            currentKeyboard = PreferenceManager
                .getDefaultSharedPreferences(a)
                .all["currentKeyboard"]?.toString() ?: defaultKeyboard!!


            val selectedPosition = listAdapter
                ?.let { adapter ->
                    (0..adapter.count - 1).find {
                        (adapter.getItem(it) as? Map<String, String>)?.get("name") == currentKeyboard
                    }
                }
                ?: NO_SELECTION

            Log.d(TAG, "selected = " + selectedPosition)
            listView.setItemChecked(selectedPosition, true)
            listView.setSelection(selectedPosition)
        }
    }

    fun doClickDeleteKeyboard(name: String) {
        try {
            Log.d(TAG, "directory item onClick: " + name)
            if (!File(
                    activity!!.getDir(APP_KEYBOARD_PATH, Context.MODE_PRIVATE),
                    name
            ).deleteRecursively()) {
                throw Exception("directory not deleted")
            }
            if (name == currentKeyboard) {
                saveKeyboardPreference(defaultKeyboard!!)
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "click delete directory failed: " + name, e)
        }
    }

    fun saveKeyboardPreference(path: String) {
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(activity).edit()
        prefEditor.putString("currentKeyboard", path)
        prefEditor.apply()
    }


}