package com.hyperana.kindleimagekeyboard
/*


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class CreateAACFragment: Fragment() {

    var fragmentListener: FragmentListener? = null


    // find preferred or default keyboard and populate aacviewmodel
    // if successful, launch AAC activity
    override fun onAttach(context: Context) {
        super.onAttach(context)


        HandlerThread("createAAC").also {
            it.start()
            Handler(it.looper)
                .post {
                    getKeyboardByPreference(context.applicationContext)
                }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_starting, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ondestroy")
    }

    fun getKeyboardByPreference(appContext: Context) : LiveData<Resource?>? {
        return App.getInstance(appContext).get("currentKeyboard")
            ?.let { it as? String }
            ?.let { keyboardName ->
                Log.i(TAG, "preferred keyboard: $keyboardName")


                AppDatabase.getDatabase(appContext)
                    ?.let { PageRepository(it) }
                    ?.let { repo ->
                        repo.listKeyboards()
                            ?.find { it.title == keyboardName }
                            ?.let {
                                Log.d(TAG, "found keyboard resource by name")
                                repo.getLiveResource(it.uid)
                            }
                    }
            }
            ?: repo.getLiveDefault(Resource.Type.KEYBOARD)
            ?.also { if (it.value == null) return null}
    }



    companion object {
        val TAG = "ChooseKeyboardFragment"
        fun create(listener: FragmentListener?) : CreateAACFragment {
            return CreateAACFragment().apply { fragmentListener = listener }
        }
    }

}*/
