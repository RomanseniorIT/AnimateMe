package com.lazuka.animateme.model

import android.view.MotionEvent
import androidx.annotation.ColorInt

sealed interface UserAction {

    data class DrawingAction(val event: MotionEvent) : UserAction

    data object UndoAction : UserAction

    data object RestoreAction : UserAction

    data object FrameDeletionAction : UserAction

    data object FrameCreationAction : UserAction

    data object PlayAction : UserAction

    data object StopAction : UserAction

    data class ColorAction(val tool: ToolsState, @ColorInt val color: Int) : UserAction

    data object FrameCopyAction : UserAction

    data object AllFramesDeletion : UserAction
}