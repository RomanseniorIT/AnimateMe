package com.lazuka.animateme.ui.model

import android.view.MotionEvent

sealed interface ViewAction {

    data class DrawingAction(val event: MotionEvent) : ViewAction

    data object UndoAction : ViewAction

    data object RestoreAction : ViewAction

    data object FrameDeletionAction : ViewAction

    data object FrameCreationAction : ViewAction

    data object PlayAction : ViewAction

    data object StopAction : ViewAction
}