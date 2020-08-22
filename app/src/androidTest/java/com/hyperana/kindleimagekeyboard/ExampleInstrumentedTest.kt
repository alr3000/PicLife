package com.hyperana.kindleimagekeyboard


import android.content.Context
import android.net.Uri
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
    val appContext = InstrumentationRegistry.getInstrumentation().context
    val app = App.getInstance(appContext)

    fun getRan() = (Math.random() * Int.MAX_VALUE).toInt()


    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            appContext, AppDatabase::class.java).build()
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
                Resource(getRan(), it.nameWithoutExtension, uri, IMAGE)
            }}
            .filterNotNull()
            .toTypedArray()

        assert(files.isNotEmpty())

        db.resourceDao().also {
            it.insertAll(*files)
            assert(it.getAllUriContains("animals").count() > 0)
        }
    }
}
