package com.lazuka.animateme.ui.model

import androidx.annotation.ColorInt

data class MainViewState(
    @ColorInt
    val color: Int,
    val previousDrawnPaths: List<DrawnPath>,
    val drawnPaths: List<DrawnPath> = emptyList(),
    val isAnimating: Boolean = false
)
