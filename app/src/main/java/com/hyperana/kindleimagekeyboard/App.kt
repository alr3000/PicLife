package com.hyperana.kindleimagekeyboard

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import java.util.*
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.LruCache
import android.widget.ImageView
import androidx.lifecycle.MutableLiveData


/**
 * Created by alr on 11/17/17.
 *
 * todo: create page from message icons
 * todo: create page from image
 * todo: export message icons or page to png.
 */
class App private constructor(val appContext: Context): SharedPreferences.OnSharedPreferenceChangeListener{
    val TAG = "App"


    var mData: MutableMap<String, Any?>? = null
    var sharedPreferences: SharedPreferences? = null
     var preferenceChangeTime: Long = 1

    var icons: List<IconData>? = null

    val iconEventLiveData = MutableLiveData<IconEvent?>(null)

    init {
        Log.d(TAG, "onCreate")

        try {
            //hashMap is inefficient - todo
            mData = hashMapOf()

            val mem = ActivityManager.MemoryInfo()
            (appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(mem)
            Log.d(TAG, "memory: available=" + mem.availMem + " threshold=" + mem.threshold + " low=" + mem.lowMemory)

            // load default settings -- false means this will not execute twice
            PreferenceManager.setDefaultValues(appContext, R.xml.settings, false)

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)

            // register listeners
           sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)


          /*  // add in defaults
            put("currentKeyboard", resources.getString(R.string.default_keyboard_name))
            put("backgroundColor", "#FF00FF")*/
        }
        catch (e: Exception) {
            Log.e(TAG, "failed create app", e)
        }
    }



    // sharedPreferences is only altered through the proper channels, but values accessed here
    fun get(key: String) : Any?{
        return sharedPreferences?.all?.get(key) ?: mData?.get(key)
    }
    fun put(key: String, value: Any?){
        mData?.put(key, value)
    }

   fun getPageList() : List<PageData> {
        return get("pageList") as? List<PageData> ?: updatePageList()
    }



    // loads stored data
    //todo: -?- page list is map by id
    fun updatePageList() : List<PageData> {

        val keyboardName = get("currentKeyboard")!! as String
        Log.d(TAG, "updatePageList: " + keyboardName)

        try {
            val newPages = jsonStringToPageDataArray(
                loadString(
                    getKeyboardConfigFile(
                        appContext,
                        keyboardName
                    ).inputStream()
                )
            )

            put("pageList", newPages)
            return newPages
        }
        catch (e: Exception) {
            Log.e(TAG, "failed load keyboard $keyboardName")
            return emptyList()
        }
    }



    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d(TAG, "onSharedPreferenceChanged")

        preferenceChangeTime = Date().time

        // make changes according to new preferences
        if (key == "currentKeyboard") {
            updatePageList()
        }

    }

    companion object : SingletonHolder<App, Context>(::App) {
        val bmpCache: LruCache<String, Bitmap> = LruCache(400) // max bitmaps in memory


        fun asyncSetImageBitmap(img: ImageView, path: String) {
            val bmp = bmpCache.get(path)
            if (bmp != null) {
                img.setImageBitmap(bmp)
                return
            }
            Log.d("IconData", "loading bitmap: " + path)
            val uiHandler = Handler()
            Thread(){
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    uiHandler.post {
                        Log.d("IconData", "bitmap loaded: " + path)
                        img.setImageBitmap(bitmap)
                        bmpCache.put(path, bitmap)
                    }
                } catch (e: Exception) {
                    Log.w("IconData", "set bitmap failed: " + path, e)
                }
            }.start()
        }


    }

}