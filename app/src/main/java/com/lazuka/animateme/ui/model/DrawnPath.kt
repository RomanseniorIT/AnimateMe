package com.lazuka.animateme.ui.model

import android.graphics.Path
import androidx.annotation.ColorInt

data class DrawnPath(
    val path: Path,

    @ColorInt
    val color: Int
) {

    // This is necessary for equals to work correctly with Path
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}