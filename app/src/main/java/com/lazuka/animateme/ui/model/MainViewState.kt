package com.lazuka.animateme.ui.model

import androidx.annotation.ColorInt

sealed interface MainViewState {

    data class Drawing(
        val frame: Frame,
        @ColorInt
        val color: Int
    ) : MainViewState {

        fun copy(drawingActions: List<DrawingAction>): Drawing {
            return copy(
                frame = frame.copy(drawingActions = drawingActions)
            )
        }
    }

    data class Animating(val frames: List<Frame>) : MainViewState
}