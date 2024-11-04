package com.lazuka.animateme.popup.frame_list.adapter

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.lazuka.animateme.databinding.RvFrameNameBinding

class FrameViewHolder(private val binding: RvFrameNameBinding) : ViewHolder(binding.root) {

    fun onBind(name: String) {
        binding.root.text = name
    }
}