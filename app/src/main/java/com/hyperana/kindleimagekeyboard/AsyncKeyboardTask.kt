package com.hyperana.kindleimagekeyboard

import android.util.Log
import android.content.Context
import android.os.AsyncTask
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.InputStream


/**
 * Created by alr on 11/5/17.
 *
 * Creates a keyboard from a directory structure
 * Handles external directories:
 *   give full path of top dir and name of the new keyboard
 *
 * or assets folders:
 *   give name (== top dir) and isAsset = true
 *
 *
 *
 * todo: -L- "duplicate" link mode and icons look different when they are links
 * todo: -?- BookData contains reference to original path, prob. other stuff eventually
 * todo: -L- lastAccessed field, description (#pages, #icons, original #cols, etc) in Keyboard class
 * todo: -?- weak reference to app context?
 */
class AsyncKeyboardParams(val appContext: Context,
                          val db: AppDatabase?,
                          val name: String,
                          val path: String = "",
                          val isAsset: Boolean = false)

open class AsyncKeyboardTask: AsyncTask<AsyncKeyboardParams, Int, String?>() {

    open val TAG = "AsyncKeyboardTask"
   // var db: RoomDatabase? = null

    // task vars:
    var selectedDirectory: File? = null
    var keyboardDirectory: File? = null
    var isAsset: Boolean = false
    var appContext: Context? = null
    var keyboardName: String = "keyboard"+createId(8)
    var appDatabase: AppDatabase? = null

    // output:
     var pageErrors = 0
    var iconErrors = 0


    override fun doInBackground(vararg params: AsyncKeyboardParams): String? {
        var name: String? = null
        try {
            Log.d(TAG, "loader doInBackground: " + params[0].toString())


            setTaskVars(params[0])



            selectedDirectory
                .let { parseDirectory() }
                .also { pages ->
                    storePages(pages, keyboardName)

                    if (isCancelled) {
                        throw Exception("cancelled")
                    }

                    appDatabase?.enterKeyboard(
                        pages,
                        Uri.fromFile(keyboardDirectory),
                        keyboardName
                    )
                }

            name = keyboardName
        }
        catch (e: Exception) {
            Log.e(TAG, "loader failed", e)
            keyboardDirectory?.deleteRecursively()
         }
        finally {
            unsetTaskVars()
            return name
        }
    }

    fun setTaskVars(params: AsyncKeyboardParams) {
        appContext = params.appContext
        keyboardName = params.name

        // input
        isAsset = params.isAsset
        selectedDirectory = if (isAsset) File("keyboards", keyboardName) else File(params.path)
        Log.d(TAG, "input -> " + selectedDirectory?.path)

        // output
        keyboardDirectory = getKeyboardSubDir(keyboardName)
        appDatabase = params.db
        Log.d(TAG, "output -> " + keyboardDirectory?.path)
    }

    fun unsetTaskVars() {
        selectedDirectory = null
        appContext = null
   }

    // ******************************** Parse Logic *************************************************

    fun parseDirectory() : List<PageData> {

        Log.d(TAG, "parseDirectory")

        // initialize output and queue
        val lPages: MutableList<PageData> = mutableListOf()

      // add first page stub to queue
        val topPage = PageData(name = keyboardName, path = keyboardDirectory!!.path)
        val queue: MutableList<Pair<File, PageData>> = mutableListOf(Pair(selectedDirectory!!,
                topPage))


        // walk directories, building and linking pages
        while (queue.count() > 0) {
            try {
                if (isCancelled) {
                    return emptyList()
                }

                val (fromDir, page) = queue.removeAt(0)


                // each item: path, PageData -> PageData(icons) with parent info,
                // add new subpage stubs to queue
                lPages.add(directoryToPageData(fromDir, page, queue))

            } catch(e: Exception) {
                Log.e(TAG, "failed create book data", e)
                pageErrors++
            }
            finally {
                (queue.count() + lPages.count()).also {
                    if (it > 0) {
                        publishProgress((lPages.count() * 100)/it, pageErrors, iconErrors)
                    }
                }
            }
        }
        Log.d(TAG, "created keyboard with " + lPages.count() + " pages")

        if (lPages.isEmpty()) {
            throw Exception("no pages created")
        }

        return lPages.toList()
    }


    protected fun directoryToPageData(fromDir: File,
                                      page: PageData,
            //   queue: MutableList<Pair<String, PageData>>)
                                      queue: MutableList<Pair<File, PageData>>)
            : PageData {

        // abstracted list files to support assets filesystem
        val files = listFiles(fromDir)
        if (files == null) {
            throw Exception("empty directory: " + fromDir.path)
        }
        Log.d(TAG, "parsing [" + fromDir.path + "]: " + files.count() + " files")

        // prepare page directory to hold icon image files:
        // top-level directory, use context.getDir(), else File(dir, name).mkdir()
        val pageDir = File(page.path)
        pageDir.mkdirs()
        if (!pageDir.exists()) {
            throw Exception("could not make internal directory: [" + page.path + "]")
        }

        var index = 0

        // todo: -?- remove items with no text
        // file filter checks extension, not isFile, to support assets
        files.filter { it.extension.isNotEmpty() }. onEach {
            try {

                // image file --> icon, internal storage bitmap
                if (it.extension in arrayOf("png", "jpg", "bmp", "jpeg", "gif")) {

                    val icon = IconData(
                            text = iconFilenameToText(it.nameWithoutExtension),
                            index = (iconFilenameToIndex(it.nameWithoutExtension)?: index).toString(),
                            fPageId = page.id
                    )

                    // copy image into internal memory and record its new path
                    val iconFile = File(pageDir, iconToFilename(icon))
                    if (!iconFile.createNewFile()) {
                        throw Exception("could not create internal file: " + iconFile.path)
                    }
                    icon.path = iconFile.path
                    copyImageTo(it, iconFile)


                    page.icons.add(icon)
                }
            }
            catch(e: Exception) {
                Log.w(TAG, "directoryToPageData file: " + it.path + " failed", e)
                iconErrors++
            }
            index++
        }

        //todo: -?- add in page.originalPath
        // stub out with id, name, parentId, add link to corresponding icon
        // file filter checks extension, not isDirectory, to support assets
        index = 0
        files.filter { it.extension.isEmpty() }.onEach {

            // dirs --> subpages
            Log.d(TAG, it.name + " isDir")
            val pageStub = PageData(name = it.name, parentPageId = page.id)

            pageStub.path = File(page.path, it.name).path

            queue.add(Pair(it, pageStub))

            index++
        }

        Log.d(TAG, "generated " + page.name + " page with " + page.icons.count() +
                " icons and " + queue.count() + " pages left")

        if ((index == 0) && (page.icons.count() == 0)) {
            // page has no icons or subpages
            throw Exception("ignoring empty page")
        }
        return page
    }



    fun listFiles(directory: File): List<File>? {
        if (isAsset) {
            return appContext!!.assets.list(directory.path)?.map { File(directory.path, it) }
        } else {
            return directory.listFiles()?.toList()
        }
    }


    fun copyImageTo(image: File, toFile: File) {

        Log.d(TAG, "copyImageTo: " + image.path + "-->" + toFile.path)

        //val stream = if (isAsset)  appContext!!.assets.open(image.path) else image.inputStream()
        val bitmap = loadResampledIcon(appContext!!, image)
        //stream.close()

        // Use the compress method on the Bitmap object to write image to
        // the OutputStream
        toFile.createNewFile()

        // all subdirs and file must be created before getting stream
        val fos = toFile.outputStream()
        Thread({
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            } catch (e: Exception) {
                Log.e(TAG, "failed copy image", e)
            } finally {
                fos.close()
            }
        }).start()
    }

    fun getKeyboardSubDir(name: String) : File? {
        val keyboardsDir = getKeyboardsDirectory(appContext!!)
        Log.d(TAG, "keyboardsDir = " + keyboardsDir.path)
        val dir = File(keyboardsDir, name)
        if (!dir.exists() && !dir.mkdir()) {
            return null
        }
        return dir
    }

    fun getFileOrAssetIS(path: String) : InputStream {
        return if (isAsset)  appContext!!.assets.open(path) else File(path).inputStream()
    }

    fun storePages(pages: List<PageData>, name: String) {
        if (isCancelled) {
            return
        }
            Log.d(TAG, "storeBookData: " + pages.count() + " items")

        // store config file in internal app data as json
        // overwrite previous
        val newDir = getKeyboardSubDir(name)
        if (newDir == null) {
            throw Exception("failed to created new keyboard folder")
        }

        val configFile = File(newDir, CONFIG_FILENAME)
        val fos = FileOutputStream(
                configFile,
                false
        )

        val json: String = JSONArray(pages.map { it.toJSONObject() }).toString(4)
        fos.write(json.toByteArray())
        fos.close()

        Log.d(TAG, "stored data in file: " + configFile.absolutePath)
    }


}