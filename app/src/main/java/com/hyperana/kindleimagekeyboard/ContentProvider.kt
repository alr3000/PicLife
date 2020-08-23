package com.hyperana.kindleimagekeyboard

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.text.isDigitsOnly
import androidx.room.Room
import androidx.room.RoomDatabase

class ContentProvider : ContentProvider() {
    val TAG = "ContentProvider"


    var db: AppDatabase? = null
    get() = field ?: buildDatabase()
        .also { field = it }

    fun buildDatabase() : AppDatabase {
        return Room.databaseBuilder(
            context!!.applicationContext!!,
            AppDatabase::class.java, "app_database"
        ).build()
    }

    override fun onCreate(): Boolean {
        Log.i(TAG, "create")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {

        return uri.lastPathSegment?.let { db?.resourceDao()?.getAllUriContainsCursor(it) }

    }


    // vnd.android.cursor.dir/vnd.com.example.provider.table1
    override fun getType(uri: Uri): String? {
        return when  {
            uri.lastPathSegment?.isDigitsOnly() == true -> "vnd.android.cursor.item/"
            else -> "vnd.android.cursor.dir/"
        }.plus("vnd.com.hyperana.resource")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        TODO("Not yet implemented")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        TODO("Not yet implemented")
    }

    companion object {
        val WORD_URI: Uri = Uri.fromParts(SCHEME_CONTENT,
            "com.hyperana.kindleimagekeyboard.word",
            null)
    }
}