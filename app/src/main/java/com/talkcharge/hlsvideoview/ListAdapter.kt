package com.talkcharge.hlsvideoview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.talkcharge.hlsvideoview.databinding.ListingLayoutBinding


class ListAdapter(val list : ArrayList<String>,val context:Context):RecyclerView.Adapter<ListAdapter.ViewHolder>() {
    class ViewHolder(val binding:ListingLayoutBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),R.layout.listing_layout,parent,false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context).load(R.drawable.placeholder).into(holder.binding.image)
        holder.binding.image.setOnClickListener {
            context.startActivity(Intent(context,PlayerActivity::class.java).putExtra("url",list[position]))
        }
    }

}