package com.hyperana.kindleimagekeyboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecentsFragment : Fragment() {

    companion object {
        fun newInstance() = RecentsFragment()
    }

    private lateinit var viewModel: RecentsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return activity?.findViewById(R.id.recents)//inflater.inflate(R.layout.recents_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

     /*  // val recyclerView: RecyclerView = view.findViewById(R.id.message_recyclerview)
        val adapter =IconListAdapter(requireActivity())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
            .apply { orientation = RecyclerView.HORIZONTAL }*/
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(RecentsViewModel::class.java)
        // TODO: Use the ViewModel
    }

}