package com.lazuka.animateme.ui.model

import android.view.MotionEvent

sealed interface UserAction {

    data class DrawingAction(val event: MotionEvent) : UserAction

    data object UndoAction : UserAction

    data object RestoreAction : UserAction

    data object FrameDeletionAction : UserAction

    data object FrameCreationAction : UserAction

    data object PlayAction : UserAction

    data object StopAction : UserAction
}