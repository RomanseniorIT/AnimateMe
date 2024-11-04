package com.lazuka.animateme.popup.frame_list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lazuka.animateme.databinding.RvFrameNameBinding

class FrameListAdapter(private val items: List<String>) : RecyclerView.Adapter<FrameViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameViewHolder {
        val binding = RvFrameNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FrameViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: FrameViewHolder, position: Int) {
        holder.onBind(items[position])
    }
}