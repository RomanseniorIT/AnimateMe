package com.lazuka.animateme.ui.model

import androidx.annotation.ColorInt

sealed interface MainViewState {

    data class Drawing(
        val frame: Frame,
        @ColorInt
        val color: Int
    ) : MainViewState {

        fun copy(drawnPaths: List<DrawnPath>): Drawing {
            return copy(
                frame = frame.copy(drawnPaths = drawnPaths)
            )
        }
    }

    data class Animating(val frames: List<Frame>) : MainViewState
}