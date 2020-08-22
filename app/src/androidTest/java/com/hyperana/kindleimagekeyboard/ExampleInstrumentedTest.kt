package com.hyperana.kindleimagekeyboard


import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    lateinit var db: AppDatabase
    lateinit var appContext: Context
    lateinit var app: App

    fun getRan() = (Math.random() * Int.MAX_VALUE).toInt()


    @Before
    fun createDb() {
        appContext = InstrumentationRegistry.getInstrumentation().context
       // app = App.getInstance(appContext)
        db = Room.inMemoryDatabaseBuilder(
            appContext, AppDatabase::class.java).build() as AppDatabase
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }




    @Test
    fun useAppContext() {
        // Context of the app under test.
        assertEquals("com.hyperana.kindleimagekeyboard", appContext.packageName)
    }


    @Test
    fun useWordDatabase() {


        listOf("happy!", "Freddy's bag", "lemons", "93", "foot massage", "now")
            .map { Word(getRan(), it, getRan())}
            .toTypedArray()
            .also {
                db.wordDao().insertAll(*it)
                assert(db.wordDao().getAll().any{ it.text == "lemons" })
            }

    }

    fun getFilesRecursive(dir: File, limit: Int) : List<File> {
        var num = 0
        return dir.listFiles()
            ?.flatMap {

                if (num < limit) {
                    if (it.isDirectory)
                        getFilesRecursive(it, limit - num).also { num += it.size }
                    else listOf(it)
                }
                else listOf()

            }
            ?.also { num += it.size }
            ?: listOf()
    }

    @Test
    fun useResourceDatabase() {

        val files = getFilesRecursive(getKeyboardsDirectory(appContext), 10)
            .map { Uri.fromFile(it)?.toString()?.let { uri ->
                Resource(getRan(), uri, IMAGE)
            }}
            .filterNotNull()
            .toTypedArray()

        assert(files.isNotEmpty())

        db.resourceDao().also {
            it.insertAll(*files)
            assert(it.getAllUriContains("animals").count() > 0)
        }
    }

    @Test
    fun storeDirectoryData() {
        val dictionary = db
        val context = appContext!!
        val loader = object: AsyncKeyboardTask() {
            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)

                // this is ui thread, so post:
                HandlerThread("boo").also {
                    it.start()
                    Handler(it.looper).post {
                        assert(dictionary.wordDao().findByText("crash", 10).count() > 0)
                    }
                }
            }
        }.apply {
            execute(AsyncKeyboardParams(
                appContext = context,
                name = "example",
                path = File(getKeyboardsDirectory(context), "example").path
            ))
        }

    }
}
