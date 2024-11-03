package com.lazuka.animateme.ui.model

import androidx.annotation.ColorInt

sealed interface DrawingViewState {

    data class Editing(
        val frame: Frame,
        @ColorInt
        val color: Int,
        val currentPosition: Int
    ) : DrawingViewState {

        fun copy(drawnPaths: List<DrawnPath>): Editing {
            return copy(
                frame = frame.copy(drawnPaths = drawnPaths)
            )
        }
    }

    data class Display(val drawnPaths: List<DrawnPath>) : DrawingViewState
}