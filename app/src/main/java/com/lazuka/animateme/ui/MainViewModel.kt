package com.lazuka.animateme.ui

import android.graphics.Color
import android.graphics.Path
import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import com.lazuka.animateme.ui.model.DrawingAction
import com.lazuka.animateme.ui.model.MainViewState
import com.lazuka.animateme.ui.model.MainViewState.Drawing
import com.lazuka.animateme.ui.model.Frame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    companion object {
        private const val FIRST_FRAME_POSITION = 0
        private const val AVAILABLE_TOUCHES_AMOUNT = 1
    }

    private val frameList = mutableListOf(Frame(FIRST_FRAME_POSITION, emptyList()))

    private val _viewStateFlow = MutableStateFlow<MainViewState>(Drawing(frameList.first(), Color.BLUE))
    val viewStateFlow: StateFlow<MainViewState> = _viewStateFlow

    fun onDrawingTouch(event: MotionEvent): Boolean {
        if (event.pointerCount > AVAILABLE_TOUCHES_AMOUNT) return false
        val currentState = _viewStateFlow.value as? Drawing ?: return false

        val x = event.x
        val y = event.y

        val actions = currentState.frame.drawingActions.toMutableList()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val path = Path()
                path.moveTo(x, y)
                actions.add(DrawingAction(path, currentState.color))
                _viewStateFlow.tryEmit(currentState.copy(actions))
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val lastAction = actions.last()
                val lastPath = lastAction.path
                lastPath.lineTo(x, y)
                val modifiedAction = lastAction.copy(path = lastPath)
                actions[actions.lastIndex] = modifiedAction
                _viewStateFlow.tryEmit(currentState.copy(actions))
            }

            else -> return false
        }
        return true
    }
}