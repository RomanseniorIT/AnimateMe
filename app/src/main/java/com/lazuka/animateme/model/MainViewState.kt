package com.lazuka.animateme.model

import androidx.annotation.ColorInt

data class MainViewState(
    @ColorInt
    val color: Int,
    val previousDrawnPaths: List<DrawnPath>,
    val drawnPaths: List<DrawnPath> = emptyList(),
    val allDrawnPath: List<DrawnPath> = emptyList(),
    val isAnimating: Boolean = false,
    val isRestoreEnabled: Boolean = false,
    val isStopEnabled: Boolean = false,
    val isPlayEnabled: Boolean = false
) {

    val isUndoEnabled = drawnPaths.isNotEmpty()
}
