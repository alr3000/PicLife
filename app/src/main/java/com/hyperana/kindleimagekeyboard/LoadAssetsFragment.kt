package com.hyperana.kindleimagekeyboard


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import java.util.*

/**
 * Created by alr on 11/13/17.
 *
 * This fragment loads the asset keyboards into internal directories equivalent to custom keyboards
 * so they appear in the keyboard preference menu, and the default is ready for use.
 *
 */

interface FragmentListener {
    fun closeFragment(fragment: Fragment)
}


class LoadAssetsFragment internal constructor(): Fragment() {

    var fragmentListener: FragmentListener? = null

    var loaders: List<AsyncTask<*,*,*>> = listOf()

    var loadersStartCount: Int = 0
    var pollTimer: Timer? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            Log.d(TAG, "onCreateView")
            return inflater.inflate(R.layout.fragment_starting, container, false)
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


            startProgress()

            //start asset move for each unloaded asset folder:
            loaders = getKeyboardsNotLoaded(requireActivity()).map {

                AsyncKeyboardTask().apply {

                    execute(
                        AsyncKeyboardParams(
                            requireActivity().applicationContext,
                            isAsset = true,
                            name = it
                        )
                    )
                }
            }

            if (loaders.isEmpty()) endStartup()
            else loadersStartCount = loaders.count()

            Log.d(TAG, "$loadersStartCount loaders started")


        }
        catch (e: Exception) {
            Log.e(TAG, "could not start load assets", e)
            endStartup()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        pollTimer?.cancel()
        pollTimer = null
        loaders.onEach {
            it.cancel(true)
        }
        loaders = listOf()

    }


    fun startProgress() {
        Log.d(TAG, "start progress")
        (view?.findViewById(R.id.progress_bar) as? ProgressBar)?.apply {
            progress = 0
            visibility = View.VISIBLE
            animate()
        }
        pollTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    updateProgress()
                }
            }, 100L, 100L)
        }
    }

    fun updateProgress() {
        loaders
            .count {
                it.status == AsyncTask.Status.FINISHED
            }
            .let {(it * 100) / loadersStartCount }
            .also {percentDone ->
                Log.d(TAG, "update progress: $percentDone")
                (view?.findViewById(R.id.progress_bar) as? ProgressBar)?.progress = percentDone
                if (percentDone == 100) endStartup()
            }
    }
    //todo: bug: "can't preform this after onsaveinstancestate" when app launched under lock screen
    fun endStartup() {
        try {
            Log.d(TAG, "endStartup")
            pollTimer?.cancel()
            pollTimer = null
            fragmentListener?.closeFragment(this)
        }
        catch (e: Exception) {
            Log.e(TAG, "could not close starting fragment", e)
        }

    }

    companion object {
        val TAG = "StartingFragment"
        fun create(listener: FragmentListener?) : LoadAssetsFragment {
            return LoadAssetsFragment().apply { fragmentListener = listener }
        }
    }
}