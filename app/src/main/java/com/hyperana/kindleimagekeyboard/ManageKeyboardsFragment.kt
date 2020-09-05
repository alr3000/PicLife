package com.hyperana.kindleimagekeyboard

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.FileObserver
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Adapter.NO_SELECTION
import androidx.fragment.app.ListFragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.fragment_manage_directories.*
import kotlinx.coroutines.*
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
            // initializeFileObserver(requireActivity(),  File(requireActivity().getDir (APP_KEYBOARD_PATH, Context.MODE_PRIVATE).absolutePath))

            //observe keyboardlist in repository:
            CoroutineScope(Dispatchers.IO).launch {
                AACRepository(AppDatabase.getDatabase(requireContext().applicationContext)!!)
                    .getLiveListKeyboards()
                    ?.observe(requireActivity()) {
                        setListViewData(it?.map { Keyboard(it) } ?: emptyList())
                    }
            }

            // set add button listener -> CreateKeyboardActivity:
            view.findViewById<Button>(R.id.button_add_directory).setOnClickListener {
                try {
                    Log.d(TAG, "onClick add directory")
                    startActivity(Intent(
                            requireActivity().applicationContext,
                            CreateKeyboardActivity::class.java
                    ))
                }
                catch (e: Exception) {
                    Log.e(TAG, "could not show choose directory dialog", e)
                }
            }

      /*      // set button listener -> SelectDropboxActivity:
            view.findViewById<Button>(R.id.button_add_dropbox).setOnClickListener {
                try {
                    Log.d(TAG, "onClick add dropbox")
                    startActivity(
                        Intent(
                            requireActivity().applicationContext,
                            SelectDropboxActivity::class.java
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "could not show choose dropbox activity", e)
                }
            }*/

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
        setListViewData(emptyList())
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
                        annotateListViewData(getDataFromFiles(activity))
                    }
                }
            }
        }



    }

    data class Keyboard(val name: String, val path: Uri, val date: Long) {
        constructor(resource: Resource) :
                this(resource.title, Uri.parse(resource.resourceUri), resource.uid.toLong())

        override fun toString(): String {
            return name
        }
    }

    fun getDataFromFiles(a: Activity) : List<Keyboard> {
        //todo: make dataclass instead of string map
        return a.getDir(APP_KEYBOARD_PATH, Context.MODE_PRIVATE).listFiles()
            ?.filter { ((it.isDirectory) && (it.name != defaultKeyboard!!)) }
            ?.map {
                Keyboard( it.name, Uri.parse(it.absolutePath), it.lastModified())
            }
            ?.sortedByDescending {
                (it.date) ?: 0
            }
            ?: emptyList()
    }

    //todo: something about offline availability
    fun annotateListViewData(alt: List<Keyboard>) {

    }

    fun setListViewData(data: List<Keyboard>) {
        Log.d(TAG, "setListViewData")
        activity?.also { a ->
            defaultKeyboard = a.resources.getString(R.string.default_keyboard_name)

            listAdapter = object : ArrayAdapter<Keyboard>(
                a,
                R.layout.directory_item,
                R.id.directory_item_path,
                data
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return super.getView(position, convertView, parent).also {view ->

                        val name = data.getOrNull(position).toString() ?: ""

                    //Can't delete default keyboard
                    view.findViewById<ImageView>(R.id.directory_item_delete)?.apply {
                        setOnClickListener {
                             doClickDeleteKeyboard(name)}
                        }
                    }


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