package com.lazuka.animateme.ui.model

import android.content.Context
import com.lazuka.animateme.R

data class Frame(
    val position: Int,
    val previousDrawnPaths: List<DrawnPath>,
    val drawnPaths: List<DrawnPath> = emptyList()
) {

    fun getName(context: Context): String = context.getString(R.string.frame_name, position)
}
