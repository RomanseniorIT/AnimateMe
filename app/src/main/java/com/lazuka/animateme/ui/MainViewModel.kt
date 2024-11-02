package com.lazuka.animateme.ui

import android.graphics.Color
import android.graphics.Path
import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazuka.animateme.ui.model.DrawnPath
import com.lazuka.animateme.ui.model.Frame
import com.lazuka.animateme.ui.model.MainViewState
import com.lazuka.animateme.ui.model.MainViewState.Drawing
import com.lazuka.animateme.ui.model.ViewAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class MainViewModel : ViewModel() {

    companion object {
        private const val FIRST_FRAME_POSITION = 0
        private const val AVAILABLE_TOUCHES_AMOUNT = 1
    }

    private val frameList = mutableListOf(Frame(FIRST_FRAME_POSITION, emptyList()))

    private val viewActionFlow = MutableSharedFlow<ViewAction>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewStateFlow: StateFlow<MainViewState> = viewActionFlow.flatMapLatest { action ->
        when (action) {
            is ViewAction.DrawingAction -> processDrawingAction(action)
            is ViewAction.UndoAction -> processUndoAction()
            is ViewAction.RestoreAction -> processRestoreAction()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Drawing(frameList.first(), Color.BLUE))

    private val drawnPathHistory = viewActionFlow
        .filterIsInstance<ViewAction.DrawingAction>()
        .map { viewStateFlow.value }
        .filterIsInstance<Drawing>()
        .map { state -> state.frame.drawnPaths }
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    private fun processDrawingAction(action: ViewAction.DrawingAction): Flow<MainViewState> {
        val event = action.event
        if (event.pointerCount > AVAILABLE_TOUCHES_AMOUNT) return viewStateFlow
        val currentState = viewStateFlow.value as Drawing

        val x = event.x
        val y = event.y

        val actions = currentState.frame.drawnPaths.toMutableList()
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val path = Path()
                path.moveTo(x, y)
                actions.add(DrawnPath(path, currentState.color))
                flowOf(currentState.copy(actions))
            }

            MotionEvent.ACTION_MOVE -> {
                val lastAction = actions.last()
                val lastPath = lastAction.path
                lastPath.lineTo(x, y)
                val modifiedAction = lastAction.copy(path = lastPath)
                actions[actions.lastIndex] = modifiedAction
                flowOf(currentState.copy(actions))
            }

            else -> viewStateFlow
        }
    }

    private fun processUndoAction(): Flow<MainViewState> {
        val state = viewStateFlow.value as Drawing
        val drawnPath = state.frame.drawnPaths.toMutableList()
        if (drawnPath.isNotEmpty()) drawnPath.removeAt(drawnPath.lastIndex)
        return flowOf(state.copy(drawnPath))
    }

    private fun processRestoreAction(): Flow<MainViewState>  {
        return drawnPathHistory.map { pathHistory ->
            val state = viewStateFlow.value
            if (state !is Drawing) return@map state

            val drawnPath = state.frame.drawnPaths.toMutableList()
            val restoredIndex = drawnPath.lastIndex + 1
            if (restoredIndex < pathHistory.size) {
                drawnPath.add(pathHistory[restoredIndex])
            }

            state.copy(drawnPath)
        }
    }

    fun onDrawingTouched(event: MotionEvent) {
        viewActionFlow.tryEmit(ViewAction.DrawingAction(event))
    }

    fun onUndoClicked() {
        viewActionFlow.tryEmit(ViewAction.UndoAction)
    }

    fun onRestoreClicked() {
        viewActionFlow.tryEmit(ViewAction.RestoreAction)
    }
}