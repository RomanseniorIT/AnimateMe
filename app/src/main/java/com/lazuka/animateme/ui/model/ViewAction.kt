package com.lazuka.animateme.ui.model

import android.view.MotionEvent

sealed interface ViewAction {

    data class DrawingAction(val event: MotionEvent) : ViewAction

    data object UndoAction : ViewAction

    data object RestoreAction: ViewAction
}