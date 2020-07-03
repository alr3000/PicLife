package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.io.File

/*import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer*/


/**
 * Created by alr on 7/25/17.
 *
 * //todo: -?- move some stuff to app subclass, Keyboard class?
 *
 */

//****************************** APP CONSTANTS **************************************************
val CONFIG_FILENAME = "config.json"
val DEFAULT_HOMEDIR = "keyboards"
val APP_KEYBOARD_PATH = "Keyboards"

val DEFAULT_ICON_TEXT = "icon" // todo: -L- change this to be less probable

val MAX_ICON_INCH = 1.5 // inches wide
val PREF_DROPBOX_ENABLED = "dropbox_enabled"
val EXTRA_ICON_ID = "icon_id"
val EXTRA_ICON_ACTION = "icon_action"


//******************************* LOAD/STORE ICON and PAGE DATA **************************************

// json string -> List<PageData>
fun jsonStringToPageDataArray(str: String): List<PageData> {
    val pageList = getListFromJSONArray(JSONArray(str)).map {
        PageData(it)
    }
    return pageList
}

// json string (array assumed) --> List of json objects
fun getListFromJSONArray(jsonArray: JSONArray) : List<JSONObject> {
    val out: MutableList<JSONObject> = mutableListOf()
    try {
        for (i in 0 .. jsonArray.length() - 1) {
            out.add(jsonArray.getJSONObject(i))
        }
    } catch(e: Exception) {
        Log.e("StaticFunctions", "problem parsing JSON list", e)
    }
    return out.toList()
}

fun loadString(stream: InputStream) : String {
    val buffer = ByteArray(stream.available())
    stream.read(buffer)
    stream.close()
    val str = String(buffer)
    return str
}

fun getKeyboardsDirectory(context: Context) : File {
    return context.getDir(APP_KEYBOARD_PATH, Context.MODE_PRIVATE)
}


fun getKeyboardsNotLoaded(context: Context) : List<String> {
    val keyboardDirs = getKeyboardsDirectory(context).list()
    return context.assets.list(DEFAULT_HOMEDIR)?.filter { !keyboardDirs.contains(it) }
        ?: emptyList()
}

fun getKeyboardConfigFile(context: Context, name: String) : File {
    val dir = File(getKeyboardsDirectory(context), name)
    return File(dir, CONFIG_FILENAME)
}

/*
// paths --> txt
fun updateHomePaths(context: Context,
                    newPaths: List<String> = listOf(),
                    removePaths: List<String> = listOf()) : List<String> {
    Log.d("StaticFunctions", "updatePaths: " + "+" + newPaths.count() + " -" + removePaths.count())
    // get old:
    val paths:MutableList<String> = if (context.fileList().contains(HOME_PATHS_FILE)) {
        loadString(context.openFileInput(HOME_PATHS_FILE))
                .split("\n")
                .toMutableList()
    } else {
        mutableListOf()
    }

    // remove as requested:
    paths.filterNot { removePaths.contains(it) }

    // put new:
    paths.addAll(newPaths)
    val stream = context.openFileOutput(HOME_PATHS_FILE, MODE_PRIVATE)
    stream.write(paths.joinToString("\n").toByteArray())

    return paths

}
*/

//***************************** PageData conventions helpers **************************************
fun createPageId() : String {
    return "page_"+createId()
}

fun createIconId(): String {
    return "icon_" + createId()
}

fun createId(length: Int = 16) : String {
    val alpha = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray()
    val numAlpha = alpha.count()
    val random = Random()
    return (0 .. length).map {
        random.nextInt()
    }.map {
        alpha[Math.abs(it%numAlpha)]
    }.joinToString("")

}

fun cleanIconText(text: String?) : String? {

    return if ((text == null) || (text.length == 0)) null else text
}

fun slug(text: String) : String {
    return text
            .replace(Regex("[^a-zA-Z0-9_ -]{1,}"), "") //replace punctuation with nothing
            .replace(Regex("[^a-zA-Z0-9-]{1,}"), "_") // replace spaces with underscore
}

fun iconToFilename(icon: IconData) : String {
    val sep = "_"
    return icon.index.toString() + sep + slug(icon.text ?: "") + ".png"
}

fun iconFilenameToIndex(fn: String): Int? {
    val sep = "_"
    val end = fn.indexOf(sep)
    return if (end < 0) null else fn.substring(0, end).toIntOrNull()
}

fun iconFilenameToText(fn: String): String? {
    val sep = "_"
    val parts = fn.split(sep)
    if (parts.firstOrNull()?.toIntOrNull() != null) {
        return parts.subList(1, parts.count()).joinToString(" ")
    }
    return parts.joinToString(" ")
}


//************************* OCR BUSINESS ********************************************************

/*fun getOCRText(textRecognizer: TextRecognizer, bmp: Bitmap ) : String?{
    var text: String? = null

    if (!textRecognizer.isOperational) {
        throw Exception("OCR not operational")
    } else {
        val frameBuilder = Frame.Builder()
        frameBuilder.setBitmap(bmp)
        val arr: SparseArray<TextBlock> = textRecognizer.detect(frameBuilder.build())
        text = ""
        (0..arr.size() - 1).onEach {
            text += arr.valueAt(it)?.value ?: ""
        }
    }

    return text
}*/

//******************************** DISPLAYS *******************************************************
fun logViewCoordinates (v: View) {
    val TAG = "StaticFunctions"
    val globalRect = Rect()
    val windowRect = intArrayOf(0,0)
    val hitRect = Rect()
    val drawRect = Rect()
    val screenRect = intArrayOf(0,0)
    v.getGlobalVisibleRect(globalRect)
    v.getHitRect(hitRect)
    v.getDrawingRect(drawRect)
    v.getLocationInWindow(windowRect)
    v.getLocationOnScreen(screenRect)
    Log.d(TAG, "view coords----")
    Log.d(TAG, "global (relative to rootView): " + globalRect)
    Log.d(TAG, "windowXY (same as global): " + windowRect.joinToString(","))
    Log.d(TAG, "hit (relative to immediate parent): " + hitRect)
    Log.d(TAG, "screenXY (relative to display screen): " + screenRect.joinToString(","))
    Log.d(TAG, "draw (relative to self!): " + drawRect)
}

fun displayInfo(context: Context, text: String) {
    Log.d("StaticFunctions", "displayInfo: " + text)
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}

// display error as toast, then throw error to get exception report from Google?
fun displayFinalError(context:Context, text: String, e:Exception?) {
    displayInfo(context, text)
    Handler().postDelayed({
        throw e ?: Exception(text)
    }, 2000)
}


/*
fun iconToImageView(context: Context, icon: IconData) : ImageView? {
    if ((icon.bmp == null) && (icon.path == null)) {
        return null
    }
    val img = ImageView(context)
    img.scaleType = ImageView.ScaleType.FIT_XY

    // set layout params to match parent: gravity = 119 (FILL)
    img.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT)//, 119)
    img.contentDescription = icon.text

        img.setImageBitmap(icon.bmp)

    return img
}*/

fun iconToTextView(context: Context, icon: IconData) : TextView? {
    //todo: -?L- long text wraps instead of shrinking, text is not centered, looks bad
    val txt = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater)
            ?.inflate(R.layout.icon_textview, null) as? TextView
    txt?.text = icon.text ?: ""
    return txt
}

// ancestor = null for offset from root
fun getOffsetFromAncestor(view: View, ancestor: View?) : Pair<Float, Float> {
    var x = 0f
    var y = 0f
    var v: View? = view

    while ((v != null) && (v != ancestor)) {
        x += v.x
        y += v.y
        v = v.parent as? View
    }
    return Pair(x, y)
}

//**************************************** BITMAP OPTIMIZATION ************************************
// ********************************* LOAD BITMAPS ****************************************
// filename --> properly sized bitmap, catch elsewhere
//http://developer.android.com/training/displaying-bitmaps/load-bitmap.html

fun loadResampledIcon(context: Context, imageFile: File) : Bitmap {
    Log.d("StaticFunctions", "loadResampledIcon")

    // Get screen dimensions for icon size and grid calculations
    val metrics = context.resources.displayMetrics
    val screenDpi = Math.max(metrics.xdpi, metrics.ydpi)
    val max_icon_px = MAX_ICON_INCH * screenDpi

    // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    decodeBitmapFileOrAsset(context, imageFile.path, options)

    // Calculate inSampleSize
    options.inSampleSize = calculateSampleSize(
            maxOf(options.outWidth, options.outHeight).toDouble(),
            max_icon_px
    )


    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false;
    return decodeBitmapFileOrAsset(context, imageFile.path, options)!!
}

// find the power of 2 reduction that most closely matches target size without going under
// for larger dimension, the assumption being they will be displayed in a square without stretching
fun calculateSampleSize(original: Double, final: Double) : Int {
    val scale = Math.floor(original/final)
    var sampleSize = 1
    while (sampleSize <= scale) {
        sampleSize = sampleSize*2
    }
    Log.d("StaticFunctions", "sampleSize = " + sampleSize.toString())
    return sampleSize
}

fun decodeBitmapFileOrAsset(context: Context, path: String, options: BitmapFactory.Options) : Bitmap? {
    if (File(path).exists()){
        Log.d("StaticFunctions", "decodeFile: " + path + " is file")
        return BitmapFactory.decodeFile(path, options)
    }
    else {
        var bmp: Bitmap? = null
        var fis: InputStream? = null

        try {
            fis = context.assets.open(path)
            Log.d("StaticFunctions", "decodeStream: " + fis)
            bmp = BitmapFactory.decodeStream(fis, null, options)
        }
        catch (e: Exception) {
            Log.w("StaticFunctions", "decodeBitmap Asset failed", e)
        }

        fis?.close()
        return bmp
    }
}
