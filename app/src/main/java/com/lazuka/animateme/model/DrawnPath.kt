package com.lazuka.animateme.model

import android.graphics.Path
import android.graphics.PorterDuffXfermode
import androidx.annotation.ColorInt

data class DrawnPath(
    val path: Path,
    @ColorInt
    val color: Int,
    val alpha: Int = 255,
    val xfermode: PorterDuffXfermode? = null,
    val startX: Float,
    val startY: Float
) {

    // This is necessary for equals to work correctly with Path
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}