package com.hyperana.kindleimagekeyboard
/*


import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class PageFragment : Fragment() {

    val TAG = "PageFragment"
    lateinit var mPage: PageData

    lateinit var aacViewModel: AACViewModel

    class ViewHolder(val view: ViewGroup, val page: PageData, aacViewModel: AACViewModel) {
        val pageView: InputPageView = view.findViewById<InputPageView>(R.id.page).apply {

            // add icon models from pagemodel's list:
            //items = page.icons

        }
    }

    fun updateView() {
        ViewHolder(view as ViewGroup, mPage, aacViewModel)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach: $tag")

        aacViewModel = ViewModelProvider(requireActivity()).get(AACViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: $tag")

        return inflater.inflate(R.layout.fragment_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: $tag")

        // attach View to live page data:
        mPage.live?.observe(viewLifecycleOwner) {
            updateView()
        }
    }

    override fun onDetach() {
        Log.d(TAG, "onDetach: $tag")
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: $tag")

        updateView()
    }

    companion object {
        fun create(page: PageData) : PageFragment {
            return PageFragment().apply {
                mPage = page
            }
        }
    }
}*/
