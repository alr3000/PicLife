package com.hyperana.kindleimagekeyboard


import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment

/**
 * Created by alr on 11/13/17.
 *
 * This fragment loads the asset keyboards into internal directories equivalent to custom keyboards
 * so they appear in the keyboard preference menu, and the default is ready for use.
 *
 */
class StartingFragment: Fragment() {
    val TAG = "StartingFragment"

    var loaders: List<AsyncTask<*,*,*>> = listOf()

    var numLoading: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            Log.d(TAG, "onCreateView")
            return inflater!!.inflate(R.layout.fragment_starting, container, false)
        }
        catch (e: Exception) {
            Log.e(TAG, "could not create fragment view", e)
            return super.onCreateView(inflater, container, savedInstanceState)
        }
    }

    // load missing asset keyboards if any
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            Log.d(TAG, "onViewCreated")

            val unloaded = getKeyboardsNotLoaded(activity!!)
            numLoading = unloaded.count()

            if (numLoading == 0) {
                Log.d(TAG, "no unloaded assets")
                endStartup()
                return
            }

            val progressBar = view.findViewById(R.id.progress_bar) as? ProgressBar
            //todo: -L- progressbar handles multiple loaders or multiple progress bars appear

            //start asset move for each unloaded asset folder:
            loaders = unloaded.map {

                val loader = object : AsyncKeyboardTask() {
                    override val TAG = "AsyncKeyboardTask: " + it

                    override fun onCancelled(result: String?) {
                        Log.d(TAG, "onCancelled")
                        super.onCancelled(result)
                    }

                    override fun onPostExecute(result: String?) {
                        Log.d(TAG, "onPostExecute")
                        super.onPostExecute(result)

                        progressBar?.progress = 100

                        numLoading -= 1
                        if (numLoading <= 0) {
                            endStartup()
                        }
                    }

                    override fun onPreExecute() {
                        Log.d(TAG, "onPreExecute")
                        super.onPreExecute()
                        progressBar?.progress = 1
                    }

                    override fun onProgressUpdate(vararg values: Int?) {
                        Log.d(TAG, "onProgressUpdate")
                        super.onProgressUpdate(*values)
                        progressBar?.progress = values[0] ?: 50
                    }
                }

                loader.execute(
                        AsyncKeyboardParams(
                                activity!!.applicationContext,
                                isAsset = true,
                                name = it
                        )
                )

                loader
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "could not start load assets", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()

        loaders.onEach {
            it.cancel(true)
        }
        loaders = listOf()
    }

    //todo: bug: "can't preform this after onsaveinstancetate" when app launched under lock screen
    fun endStartup() {
        try {
            Log.d(TAG, "endStartup")
           fragmentManager?.beginTransaction()?.remove(this)?.commit()
        }
        catch (e: Exception) {
            Log.e(TAG, "could not close starting fragment", e)
        }

    }
}