package com.hyperana.kindleimagekeyboard

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.os.FileObserver
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Adapter.NO_SELECTION
import androidx.fragment.app.ListFragment
import androidx.preference.PreferenceManager
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
            val view = inflater.inflate(R.layout.fragment_manage_directories, container!!, false)

            // watch for delete events. create events happen while observer is paused.
            initializeFileObserver(requireActivity(),  File(requireActivity().getDir (APP_KEYBOARD_PATH, Context.MODE_PRIVATE).absolutePath))

            // set add button listener -> CreateKeyboardActivity:
            view.findViewById<Button>(R.id.button_add_directory).setOnClickListener {
                try {
                    Log.d(TAG, "onClick add directory")
                    startActivity(Intent(
                            requireActivity().applicationContext,
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
            val name = (listAdapter!!.getItem(position) as Map<*,*>)["name"] as String
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
    fun initializeFileObserver(activity: Activity, file: File) {

            dataFilesObserver = object : FileObserver(file.path) {

            override fun onEvent(event: Int, path: String?) {
                Log.d(TAG, "dataFileObserver onEvent: " + event + " on " + path)

                // mask event to exclude special directory, etc, bits
                if ((event and FileObserver.CREATE == FileObserver.CREATE)
                    || (event and FileObserver.DELETE == FileObserver.DELETE)
                ) {

                    // fileObserver is in a different thread
                    activity.runOnUiThread {
                        setListViewData()
                    }
                }
            }
        }

    }

    fun setListViewData() {
        Log.d(TAG, "setListViewData")
        activity?.also { a ->
            defaultKeyboard = a.resources.getString(R.string.default_keyboard_name)

            //todo: make dataclass instead of string map
            val data = a.getDir(APP_KEYBOARD_PATH, Context.MODE_PRIVATE).listFiles()
                ?.filter { ((it.isDirectory) && (it.name != defaultKeyboard!!)) }
                ?.map {
                    mapOf(
                        Pair("path", it.path),
                        Pair("name", it.name),
                        Pair("date", it.lastModified())
                    )
                }
                ?.sortedByDescending {
                    (it["date"] as? Long) ?: 0
                }
                ?: emptyList()

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

                    val name = (this.getItem(position) as? Map<*,*>)?.get("name") as? String

                    //Handle TextView and display string from your list
                    val listItemText = view?.findViewById(R.id.directory_item_path) as? TextView
                    listItemText?.setText(name ?: "NO NAME")

                    //Can't delete default keyboard
                    view?.findViewById<ImageView>(R.id.directory_item_delete)?.apply {
                        setOnClickListener {
                            name?.also { doClickDeleteKeyboard(name)}
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
                    (0 until adapter.count).find {
                        ((adapter.getItem(it) as? Map<*,*>)?.get("name") as? String)?.equals(currentKeyboard) == true
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
            Log.d(TAG, "directory item onClick:  $name")
            if (!File(
                    requireActivity().getDir(APP_KEYBOARD_PATH, Context.MODE_PRIVATE),
                    name
            ).deleteRecursively()) {
                throw Exception("directory not deleted")
            }
            if (name == currentKeyboard) {
                saveKeyboardPreference(defaultKeyboard!!)
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "click delete directory failed: $name", e)
        }
    }

    fun saveKeyboardPreference(path: String) {
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(activity).edit()
        prefEditor.putString("currentKeyboard", path)
        prefEditor.apply()
    }


}