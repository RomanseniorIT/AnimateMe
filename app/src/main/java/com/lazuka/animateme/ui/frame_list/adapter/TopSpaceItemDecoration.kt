package com.lazuka.animateme.ui.frame_list.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.lazuka.animateme.R

class TopSpaceItemDecoration : ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val viewHolder = parent.getChildViewHolder(view)
        val adapterPosition = viewHolder.adapterPosition
        val isHolderInAdapter = adapterPosition != NO_POSITION
        val position = if (isHolderInAdapter) adapterPosition else viewHolder.layoutPosition

        if (position != 0) {
            val margin = parent.resources.getDimensionPixelSize(R.dimen.space)
            outRect.top = margin
        }
    }
}