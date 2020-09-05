package com.hyperana.kindleimagekeyboard


import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.io.File
import java.io.IOException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private val assetKeyboardName = "example"
    private lateinit var db: AppDatabase
    private lateinit var appContext: Context

   // private lateinit var testFileDir: File

    private fun getRan() = (Math.random() * Int.MAX_VALUE).toInt()


    @Before
    fun createDb() {
        appContext = androidx.test.core.app.ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        try {
            File(getKeyboardsDirectory(appContext), assetKeyboardName).deleteRecursively()
        }
        catch (e: Exception) { println("Output Directory not deleted: " + e.message)}
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
        try {
            File(getKeyboardsDirectory(appContext), assetKeyboardName).deleteRecursively()
        }
        catch (e: Exception) { println("Output Directory not deleted: " + e.message)}
    }




    @Test
    fun useAppContext() {
        // Context of the app under test.
        assertEquals("com.hyperana.piclife", appContext.packageName)
    }


    @Test
    fun useWordDatabase() {


        listOf("happy!", "Freddy's bag", "lemons", "93", "foot massage", "now")
            .map { Word(getRan(), it, getRan(), 1)}
            .toTypedArray()
            .also {
                db.wordDao().insertAll(*it)
                assertEquals(db.wordDao().getAllByText("lemons", 10)?.let { list -> list.count > 0 }, true)
            }

    }

    fun getFilesRecursive(dir: File, limit: Int) : List<File> {
        var num = 0
        return dir.listFiles()
            ?.flatMap {file ->

            if (num < limit) {
                    if (file.isDirectory)
                        getFilesRecursive(file, limit - num).also { num += it.size }
                    else listOf(file)
                }
                else listOf()

            }
            ?.also { num += it.size }
            ?: listOf()
    }

    @Test
    fun useResourceDatabase() {

        val files = getFilesRecursive(getKeyboardsDirectory(appContext), 10)
            .mapNotNull {
                Uri.fromFile(it)?.toString()?.let { uri ->
                    Resource(getRan(), uri, Resource.Type.IMAGE.name)
                }
            }
            .toTypedArray()

        assertEquals(files.isNotEmpty(), true)

        db.resourceDao().also {dao ->
            dao.upsert(files.asList())
            assertEquals(dao.getAllUriContains("animals")?.let { it.count > 0}, true)
        }


    }

    @Test
    fun useKeyboardResource() {
        useAppContext()

        storeDirectoryData {
            System.out.println("found " + db.resourceDao().getCount() + " total resources")
            System.out.println("first keyboard: " + db.resourceDao().getLiveAny(Resource.Type.KEYBOARD.name)?.value?.title)
        }
    }

    fun getChildrenFromList(db: AppDatabase, list: List<Resource>) : List<Resource> {

        assertEquals(true, list.size > 0)
        return list
            .flatMap { it.children.split(AppDatabase.DELIMITER).mapNotNull { it.toIntOrNull() } }
            .let {
                println("found ${it.size} children")
                db.resourceDao().listAllByIds(it.toIntArray())
            }
    }

    fun storeDirectoryData(cb: () -> Unit) {
        val dictionary = db
        val context = appContext
        object: AsyncKeyboardTask() {
            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)

                // this is ui thread, so post database lookup:
                HandlerThread("boo").also {
                    it.start()
                    Handler(it.looper).post {
                        System.out.println("checking stored resources")
                        assertEquals(true, dictionary.wordDao().listByText("need").count() > 0)
                        cb()
                    }
                }
            }
        }.apply {
            execute(AsyncKeyboardParams(
                appContext = context,
                db = db,
                name = assetKeyboardName,
                isAsset = true
            ))
        }

    }

    @Test
    fun useContentProvider() {
        storeDirectoryData() {

            val cursor = appContext.contentResolver.query(
                ContentProvider.WORD_URI.buildUpon().appendPath("crash").build(),
                null, null, null, null
            )

            assertEquals(cursor?.count?.let { it > 0 }, true)
        }
    }

    @Test
    fun useRecentsTable() {
        val dao = db.recentDao()
        println("Use recents table")

        Array<Int>(8) { it }.forEach {
            if (it == 4) dao.clearRecents()
            else if (it == 6) dao.startMessage()
            else dao.insert(Recent(resourceId = it, actionType = Recent.ActionType.ADD_TO_MESSAGE.ordinal))
        }

        dao.getAllSince(Recent.ActionType.START_MESSAGE.ordinal)
            .also { list -> println("Since message: ["+list.map { it.actionType }.joinToString() + "]") }
            .also { assertEquals(it.count(), 2 )}
        dao.getAllSince(Recent.ActionType.CLEAR_RECENTS.ordinal)
            .also { list -> println("Since clear: [" + list.map { it.actionType }.joinToString() + "]") }
            .also { assertEquals(it.count(), 4) }

    }
}
