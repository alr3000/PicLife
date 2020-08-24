package com.hyperana.kindleimagekeyboard

import android.content.ContentProvider
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.text.isDigitsOnly
import androidx.room.Room

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

        return when {
            // use Word table:
            uri.pathSegments.contains(WORD) -> {

                // path is id:
                if (uri.pathSegments.contains(ITEM)) db?.wordDao()
                    ?.getAllByIds(intArrayOf(uri.lastPathSegment?.toIntOrNull() ?: 0))

                // path is search term:
                else db?.wordDao()?.getAllByText(uri.lastPathSegment ?: "no_text", 10)
            }

            // perform search for word-associated resource records:
            uri.pathSegments.contains(SEARCH) -> {
                uri.lastPathSegment?.let { db?.getResourcesByWord(it) }
            }

            // perform query on resource table based on path:
            else -> {
                // path is id:
                if (uri.pathSegments.contains(ITEM)) db?.resourceDao()
                    ?.get(uri.lastPathSegment?.toIntOrNull() ?: 0)

                // path is resourceType:
                else uri.lastPathSegment?.let {db?.resourceDao()?.getAllByType(arrayOf(it))}
            }

        }

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

        val authority = "com.hyperana.piclife.ContentProvider"

        // return records from WordDao:
        val WORD = "word"

        // return resource children of matching word:
        val SEARCH = "search"

        // return single record with given id:
        val ITEM = "item"

        val WORD_URI: Uri = Uri.Builder()
            .scheme(SCHEME_CONTENT)
            .authority(authority)
            .appendPath("word")
            .build()

    }
}