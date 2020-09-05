package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import java.io.File


class CreateKeyboardActivity : AppCompatActivity() {

    val TAG = "CreateKeyboardActivity"
    val REQUEST_SELECT_DROPBOX = 7324

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_create_keyboard)


            //DropBox:
          //  Log.d(TAG, "DropBox Auth: ${BuildConfig.CONSUMER_KEY}")
            Log.d(TAG, "DropboxApiKey: " + resources.getString(R.string.app_key))
            /* val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }*/

            val pathInput = findViewById(R.id.edit_keyboard_path) as EditText
            val pathError = findViewById<TextView>(R.id.error_keyboard_path)
            val saveButton = findViewById(R.id.button_save_keyboard) as Button
            val nameInput = findViewById(R.id.edit_keyboard_name) as EditText
            val nameError = findViewById<TextView>(R.id.error_keyboard_name)
            val dropboxUI = findViewById<View>(R.id.dropbox_container)
            val enableDBButton = findViewById<View>(R.id.button_enable_dropbox).apply {
                visibility = if (PreferenceManager
                        .getDefaultSharedPreferences(this@CreateKeyboardActivity)
                        .getBoolean(PREF_DROPBOX_ENABLED, false)) View.GONE else View.VISIBLE
                setOnClickListener {
                    Log.d(TAG, "start enableDropboxactivity for result...")
                    try {
                        startActivityForResult(
                            Intent().setClassName(packageName, SelectDropboxActivity::class.java.name),
                            REQUEST_SELECT_DROPBOX
                        )
                    }
                    catch (e: Exception) {
                        Log.e(TAG, "Failed start dropbox activity", e)
                    }
                }
            }


            // set listener for keyboard path
            (pathInput).setOnClickListener { v ->
                try {

                        Log.d(TAG, "pathInput onClick")
                        pathError?.visibility = View.INVISIBLE

                        showPickDirectoryDialog(object : DialogSelectionListener {

                            // An array of paths is returned
                            override fun onSelectedFilePaths(p0: Array<out String>?) {
                                Log.d(TAG, "onSelectedFilePaths: " + p0?.joinToString(", "))
                                try {
                                    if ((p0 != null) && (!p0.isEmpty())) {
                                        val editor = pathInput.text
                                        editor.clear()
                                        editor.insert(0, p0.get(0))
                                    }
                                } catch(e: Exception) {
                                    Log.e(TAG, "failed reload.", e)
                                }
                            }
                        })

                } catch (e: Exception) {
                    Log.e(TAG, "failed show dialog", e)
                }
            }

            // set name listener: validate and show error, modify save label
           nameInput.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    Log.d(TAG, "nameInput afterTextChanged")
                    nameError?.visibility =
                            if (validateName(nameInput.text.toString())) View.INVISIBLE
                            else View.VISIBLE
                    saveButton.setText(
                            if (isNameExists(nameInput.text.toString()))
                                R.string.button_overwrite
                            else
                                R.string.button_save
                    )
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // set listener for save button
            saveButton.setOnClickListener {
                try {
                    Log.d(TAG, "saveButton onClick")
                    val name = nameInput.text.toString() //Uri.encode(nameInput.text.toString().trim())
                    val path = pathInput.text.toString()
                    if (!validateName(name)) {
                        nameError?.visibility = View.VISIBLE
                    } else if (!validatePath(path)) {
                        pathError?.visibility = View.VISIBLE
                    } else {
                        doSaveKeyboard(name = name, path = path)
                    }
                }
                catch (e: Exception) {
                    Log.e(TAG, "failed save", e)
                }
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "failed create", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: $requestCode, $resultCode, ${data?.data}")
    }


    fun validateName(name: String?) : Boolean {
        val list: List<String?> = listOf(null, "") // and all the existing names
        return !(list.contains(name))
    }

    fun isNameExists(name: String?) : Boolean {
        return getDir(APP_KEYBOARD_PATH, Context.MODE_PRIVATE).list()?.contains(name) == true
    }

    fun validatePath(path: String?) : Boolean {
        return !((path == null) || path.isEmpty())
    }

    fun showPickDirectoryDialog(listener: DialogSelectionListener) {
        val properties = DialogProperties()

        properties.selection_mode = DialogConfigs.MULTI_MODE //doesn't select dirs without the checkboxes
        properties.selection_type = DialogConfigs.DIR_SELECT
        properties.root = File(DialogConfigs.DEFAULT_DIR)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)
        properties.extensions = null

// Next create an instance of FilePickerDialog,
// and pass Context and DialogProperties references as parameters.
// Optional: You can change the title of dialog. Default is current directory name.
// Set the positive button string. Default is Select. Set the negative button string.
// Defalut is Cancel.

        val dialog = FilePickerDialog(this, properties)
        dialog.setTitle(this.resources.getString(R.string.choose_load_keyboard))
        dialog.setDialogSelectionListener(listener)

        dialog.show()
    }

    // run loader/saver:
    // success -> end fragment
    // fail -> show error
    fun doSaveKeyboard(name: String, path: String) {
        Log.d(TAG, "doSaveKeyboard: " + name + " - " + path)
        // run loader

        val progress = findViewById(R.id.progress_bar) as ProgressBar
        val savebutton = findViewById(R.id.button_save_keyboard) as Button
        val db = AppDatabase.getDatabase(applicationContext)

        val loader = object: AsyncKeyboardTask() {
            override fun onPreExecute() {
                super.onPreExecute()
                savebutton.isEnabled = false
                progress.visibility = View.VISIBLE
            }

            override fun onProgressUpdate(vararg values: Int?) {
                super.onProgressUpdate(*values)
                progress.progress = values[0] ?: 50
            }

            override fun onCancelled(result: String?) {
                super.onCancelled(result)
                savebutton.isEnabled = true
                progress.visibility = View.INVISIBLE
            }

            //
            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                savebutton.isEnabled = true
                if (result == null) {
                    Toast.makeText(
                            this@CreateKeyboardActivity,
                            "Failed to create keyboard",
                            Toast.LENGTH_SHORT).show()
                    return
                }
                    Toast.makeText(
                            this@CreateKeyboardActivity,
                            name + " keyboard created" + if (this.pageErrors > 0) " with some errors" else "",
                            Toast.LENGTH_SHORT).show()
                    onBackPressed()
                }
        }

        loader.execute(AsyncKeyboardParams(
            appContext = applicationContext,
            db = db,
            name = name,
            path = path
        ))
    }


}
