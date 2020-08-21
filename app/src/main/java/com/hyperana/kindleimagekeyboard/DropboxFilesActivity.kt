package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import com.dropbox.core.examples.android.FilesActivity

class DropboxFilesActivity : FilesActivity() {

    val TAG = "DropboxFilesActivity"

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        Log.d(TAG, "onCreate")


    }

    companion object {
        fun getIntent(context: Context?, path: String?): Intent? {
            val filesIntent = Intent(context, DropboxFilesActivity::class.java)
            filesIntent.putExtra(FilesActivity.EXTRA_PATH, path)
            return filesIntent
        }
    }
}