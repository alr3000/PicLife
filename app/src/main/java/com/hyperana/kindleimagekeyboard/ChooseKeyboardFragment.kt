package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.fragment_choose_keyboard.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ChooseKeyboardFragment internal constructor(): Fragment(), AdapterView.OnItemClickListener {

    var fragmentListener: FragmentListener? = null

    lateinit var repo: AACRepository
    var keyboards: List<Resource> = listOf()
    set (value) {
        field = value
        onSetKeyboards()
    }

    init { Log.i(TAG, "init")}

    override fun onAttach(context: Context) {
        super.onAttach(context)
        repo = AACRepository(AppDatabase.getDatabase(context.applicationContext)!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            Log.d(TAG, "onCreateView")
            return inflater.inflate(R.layout.fragment_choose_keyboard, container, false)
        }
        catch (e: Exception) {
            Log.e(TAG, "could not create fragment view", e)
            return super.onCreateView(inflater, container, savedInstanceState)
        }
    }

    // load missing asset keyboards if any
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onResume() {
        super.onResume()
        try {
            Log.d(TAG, "onViewCreated")


            CoroutineScope(Dispatchers.IO).launch {
                keyboards = repo.listKeyboards() ?: listOf()
            }


        }
        catch (e: Exception) {
            Log.e(TAG, "failed resume choose keyboard fragment", e)
        }
    }

    fun onSetKeyboards() {

        // set current selection before setting selection listener:
        val selected = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireActivity().applicationContext)
            .getString(PREF_KEYBOARD_ID, null)
            ?.let { id ->
                keyboards.indexOfFirst { it.uid == id.toIntOrNull() ?: -1 }
            }

        CoroutineScope(Dispatchers.Main).launch {
            view?.findViewById<ListView>(R.id.keyboard_list)?.apply {
                adapter = object : ArrayAdapter<Resource>(
                    requireActivity(), android.R.layout.simple_list_item_1, android.R.id.text1,
                    keyboards
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        return super.getView(position, convertView, parent).apply {
                            findViewById<TextView>(android.R.id.text1)
                                ?.text = keyboards.getOrNull(position)?.title ?: "No title"
                            isSelected = position == selected
                        }
                    }

                    override fun getItemId(position: Int): Long {
                        return keyboards.getOrNull(position)?.uid?.toLong() ?: 0L
                    }
                }



                onItemClickListener = this@ChooseKeyboardFragment
            }
        }
    }


    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.i(TAG, "item selected: $position")
        keyboard_list.setSelection(position)
        PreferenceManager.getDefaultSharedPreferences(requireActivity().applicationContext).edit()
            .putString(PREF_KEYBOARD_ID, id.toString())
            .apply()
        fragmentListener?.closeFragment(this)
    }


    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()


    }


    companion object {
        val TAG = "ChooseKeyboardFragment"
        fun create(listener: FragmentListener?) : ChooseKeyboardFragment {
            return ChooseKeyboardFragment().apply { fragmentListener = listener }
        }
    }
}