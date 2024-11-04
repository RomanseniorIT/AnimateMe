package com.lazuka.animateme.popup.frame_list

import android.widget.LinearLayout
import android.widget.PopupWindow
import com.lazuka.animateme.R
import com.lazuka.animateme.databinding.PopupFrameListBinding
import com.lazuka.animateme.popup.frame_list.adapter.FrameListAdapter
import com.lazuka.animateme.popup.frame_list.adapter.TopSpaceItemDecoration

class FrameListPopupWindow(
    binding: PopupFrameListBinding,
    frames: List<String>
) : PopupWindow(
    binding.root,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    true
) {

    init {
        contentView.setBackgroundResource(R.drawable.bg_popup)
        binding.rvItems.adapter = FrameListAdapter(frames)
        binding.rvItems.addItemDecoration(TopSpaceItemDecoration())
    }
}