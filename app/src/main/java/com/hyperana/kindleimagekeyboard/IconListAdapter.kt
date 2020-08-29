package com.hyperana.kindleimagekeyboard

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IconListAdapter(context: Context):  RecyclerView.Adapter<IconListAdapter.IconViewHolder>() {

    class IconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView = view.findViewById<TextView>(R.id.icon_text)
        val thumbView = view.findViewById<ImageView>(R.id.icon_thumb)
        fun setText(text: String?) = textView?.setText(text)
        fun setThumb(uri: Uri?)  {
          //  thumbView?.also { App.asyncSetImageBitmap(it, uri)}
        }
    }

    private final var mInflater: LayoutInflater = LayoutInflater.from(context)
    private var icons: List<IconData>  = listOf()

    init { mInflater = LayoutInflater.from(context); }

    fun setIcons(list: List<IconData>) {
        icons = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        return mInflater.inflate(R.layout.element_icon, parent, false)
            .let { IconViewHolder(it)}

    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        icons.getOrNull(position).also { icon ->
            holder.setText(icon?.text)
            holder.setThumb(icon?.thumbUri)
        }
    }

    override fun getItemCount(): Int {
        return icons.count()
    }



}

