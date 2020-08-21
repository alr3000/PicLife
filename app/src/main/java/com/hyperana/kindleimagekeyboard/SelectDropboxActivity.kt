package com.hyperana.kindleimagekeyboard

import android.os.Bundle
import android.util.Log
import com.dropbox.core.examples.android.FilesActivity
import com.dropbox.core.examples.android.FilesActivity.getIntent
import com.dropbox.core.examples.android.UserActivity

class SelectDropboxActivity : UserActivity() {
    val TAG = "SelectDropboxActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate:")


        super.onCreate(savedInstanceState)
    }

    override fun loadData() {
        Log.d(TAG, "loadData")

        this.startActivity(FilesActivity.getIntent(this, ""))

    }
}
