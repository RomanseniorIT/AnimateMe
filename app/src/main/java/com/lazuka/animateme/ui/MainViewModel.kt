package com.lazuka.animateme.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazuka.animateme.R
import com.lazuka.animateme.ui.model.DrawnPath
import com.lazuka.animateme.ui.model.Frame
import com.lazuka.animateme.ui.model.DrawingViewState
import com.lazuka.animateme.ui.model.DrawingViewState.Editing
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class MainViewModel : ViewModel() {

    companion object {
        private const val FIRST_FRAME_POSITION = 0
        private const val AVAILABLE_TOUCHES_AMOUNT = 1
        private const val PREVIOUS_FRAME_ALPHA = 100
    }

    private val frameList = mutableListOf(Frame(emptyList()))

    private val viewActionFlow = MutableSharedFlow<ViewAction>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val drawingStateFlow: StateFlow<DrawingViewState> = viewActionFlow
        .flatMapLatest { action ->
            when (action) {
                is ViewAction.DrawingAction -> processDrawingAction(action)
                is ViewAction.UndoAction -> processUndoAction()
                is ViewAction.RestoreAction -> processRestoreAction()
                is ViewAction.FrameDeletionAction -> processFrameDeletionAction()
                is ViewAction.FrameCreationAction -> processFrameCreationAction()
            }
        }
        .onEach { state -> if (state is Editing) frameList[state.currentPosition] = state.frame }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            Editing(frameList.first(), Color.BLUE, FIRST_FRAME_POSITION)
        )

    private val drawnPathHistory = viewActionFlow
        .filterIsInstance<ViewAction.DrawingAction>()
        .map { drawingStateFlow.value }
        .filterIsInstance<Editing>()
        .map { state -> state.frame.drawnPaths }
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    private fun processDrawingAction(action: ViewAction.DrawingAction): Flow<DrawingViewState> {
        val event = action.event
        if (event.pointerCount > AVAILABLE_TOUCHES_AMOUNT) return drawingStateFlow
        val currentState = drawingStateFlow.value as Editing

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

            else -> drawingStateFlow
        }
    }

    private fun processUndoAction(): Flow<DrawingViewState> {
        val state = drawingStateFlow.value as Editing
        val drawnPath = state.frame.drawnPaths.toMutableList()
        if (drawnPath.isNotEmpty()) drawnPath.removeAt(drawnPath.lastIndex)
        return flowOf(state.copy(drawnPath))
    }

    private fun processRestoreAction(): Flow<DrawingViewState> {
        return drawnPathHistory.map { pathHistory ->
            val state = drawingStateFlow.value as Editing
            val drawnPath = state.frame.drawnPaths.toMutableList()
            val restoredIndex = drawnPath.lastIndex + 1
            if (restoredIndex < pathHistory.size) {
                drawnPath.add(pathHistory[restoredIndex])
            }

            state.copy(drawnPath)
        }
    }

    private fun processFrameDeletionAction(): Flow<DrawingViewState> {
        val state = drawingStateFlow.value as Editing
        val position = state.currentPosition - 1

        val resultState = if (frameList.size == 1) {
            frameList[FIRST_FRAME_POSITION] = Frame(emptyList())
            state.copy(frame = frameList.first(), currentPosition = FIRST_FRAME_POSITION)
        } else {
            frameList.removeAt(frameList.lastIndex)
            state.copy(frame = frameList.last(), currentPosition = position)
        }

        return flowOf(resultState)
    }

    private fun processFrameCreationAction(): Flow<DrawingViewState> {
        val state = drawingStateFlow.value as Editing
        val prevFrame = state.frame
        val frame = Frame(prevFrame.drawnPaths.map { it.copy(alpha = PREVIOUS_FRAME_ALPHA) }) // TODO("Think about it")
        val newPosition = state.currentPosition + 1
        if (newPosition < frameList.lastIndex) {
            frameList.add(newPosition, frame)
        } else {
            frameList.add(frame)
        }

        return flowOf(state.copy(frame = frameList.last(), currentPosition = newPosition))
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

    fun onDeleteFrameClicked() {
        viewActionFlow.tryEmit(ViewAction.FrameDeletionAction)
    }

    fun onCreateFrameClicked() {
        viewActionFlow.tryEmit(ViewAction.FrameCreationAction)
    }

    fun getDisplayFrames(context: Context): List<String> {
        return frameList.indices.map { position -> context.getString(R.string.frame_name, position + 1) }
    }
}