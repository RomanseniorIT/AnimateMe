package com.lazuka.animateme

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazuka.animateme.model.DrawnPath
import com.lazuka.animateme.model.MainViewState
import com.lazuka.animateme.model.ToolsState
import com.lazuka.animateme.model.UserAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MainViewModel : ViewModel() {

    companion object {
        private const val FIRST_FRAME_POSITION = 0
        private const val AVAILABLE_TOUCHES_AMOUNT = 1
        private const val PREVIOUS_FRAME_ALPHA = 100
        private const val FRAME_DELAY = 500L
    }

    private val toolsStateFlow = MutableStateFlow(ToolsState.CLEARED)
    val clearToolsFlow = toolsStateFlow.filter { tool -> tool == ToolsState.CLEARED }
    val showToolsPopupFlow = toolsStateFlow.filter { tool -> tool == ToolsState.TOOLS }
    val toolsButtonFlow = toolsStateFlow.filter { tool -> tool != ToolsState.CLEARED }
    val showColorsPopupFlow = toolsStateFlow.filter { tool -> tool == ToolsState.COLORS }
    val colorsButtonFlow = toolsStateFlow.filter { tool ->
        tool == ToolsState.WHITE || tool == ToolsState.RED || tool == ToolsState.BLACK || tool == ToolsState.BLUE
    }

    private val frameList = mutableListOf(MainViewState(Color.BLUE, emptyList()))

    private val userActionFlow = MutableSharedFlow<UserAction>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewStateFlow: StateFlow<MainViewState> = userActionFlow
        .flatMapLatest { action ->
            when (action) {
                is UserAction.DrawingAction -> processDrawingAction(action)
                is UserAction.UndoAction -> processUndoAction()
                is UserAction.RestoreAction -> processRestoreAction()
                is UserAction.FrameDeletionAction -> processFrameDeletionAction()
                is UserAction.FrameCreationAction -> processFrameCreationAction()
                is UserAction.PlayAction -> processPlayAction()
                is UserAction.StopAction -> processStopAction()
                is UserAction.ColorAction -> processColorAction(action.color, action.tool)
            }
        }
        .onEach { state -> if (!state.isAnimating) frameList[frameList.lastIndex] = state }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), frameList.first())

    private val drawnPathHistory = userActionFlow
        .filter { action -> action is UserAction.DrawingAction }
        .map { viewStateFlow.value }
        .filterIsInstance<MainViewState>()
        .map { state -> state.drawnPaths }
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    private val lastEditingState = viewStateFlow
        .filter { state -> state.isAnimating.not() }
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    private fun processDrawingAction(action: UserAction.DrawingAction): Flow<MainViewState> {
        val toolsState = toolsStateFlow.value
        val mode = when (toolsState) {
            ToolsState.CLEARED, ToolsState.TOOLS -> return viewStateFlow
            ToolsState.ERASER -> PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            else -> null
        }

        val event = action.event
        if (event.pointerCount > AVAILABLE_TOUCHES_AMOUNT) return viewStateFlow

        val currentState = viewStateFlow.value
        val x = event.x
        val y = event.y

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val actions = currentState.drawnPaths.toMutableList()
                val path = Path()
                when (toolsState) {
                    ToolsState.PENCIL, ToolsState.LINE, ToolsState.ERASER -> path.moveTo(x, y)
                    ToolsState.CIRCLE -> path.addCircle(x, y, 0f, Path.Direction.CW)
                    else -> Unit
                }

                actions.add(
                    DrawnPath(
                        path = path,
                        color = currentState.color,
                        xfermode = mode,
                        startX = x,
                        startY = y
                    )
                )
                flowOf(currentState.copy(drawnPaths = actions))
            }

            MotionEvent.ACTION_MOVE -> {
                val actions = currentState.drawnPaths.toMutableList()
                val lastAction = actions.last()
                val lastPath = lastAction.path
                when (toolsState) {
                    ToolsState.PENCIL, ToolsState.ERASER -> lastPath.lineTo(x, y)
                    ToolsState.CIRCLE -> {
                        val startX = lastAction.startX
                        val startY = lastAction.startY
                        val radius = sqrt(abs(startX - x).pow(2f) + abs(startY - y).pow(2f))
                        lastPath.reset()
                        lastPath.addCircle(startX, startY, radius, Path.Direction.CW)
                    }

                    ToolsState.LINE -> {
                        lastPath.reset()
                        lastPath.moveTo(lastAction.startX, lastAction.startY)
                        lastPath.lineTo(x, y)
                    }

                    ToolsState.RECTANGLE -> {
                        val left = min(lastAction.startX, x)
                        val right = max(lastAction.startX, x)
                        val top = min(lastAction.startY, y)
                        val bottom = max(lastAction.startY, y)
                        lastPath.reset()
                        lastPath.addRect(left, top, right, bottom, Path.Direction.CW)
                    }

                    else -> Unit
                }

                val modifiedAction = lastAction.copy(path = lastPath)
                actions[actions.lastIndex] = modifiedAction
                flowOf(currentState.copy(drawnPaths = actions))
            }

            else -> viewStateFlow
        }
    }

    private fun processUndoAction(): Flow<MainViewState> {
        val state = viewStateFlow.value
        val drawnPath = state.drawnPaths.toMutableList()
        if (drawnPath.isNotEmpty()) drawnPath.removeAt(drawnPath.lastIndex)

        return flowOf(state.copy(drawnPaths = drawnPath))
    }

    private fun processRestoreAction(): Flow<MainViewState> {
        return drawnPathHistory.map { pathHistory ->
            val state = viewStateFlow.value
            val drawnPath = state.drawnPaths.toMutableList()
            val restoredIndex = drawnPath.lastIndex + 1
            if (restoredIndex < pathHistory.size) {
                drawnPath.add(pathHistory[restoredIndex])
            }

            state.copy(drawnPaths = drawnPath)
        }
    }

    private fun processFrameDeletionAction(): Flow<MainViewState> {
        val newState = if (frameList.size == 1) {
            frameList[FIRST_FRAME_POSITION] = MainViewState(Color.BLUE, emptyList())
            frameList.first()
        } else {
            frameList.removeAt(frameList.lastIndex)
            frameList.last()
        }

        return flowOf(newState)
    }

    private fun processFrameCreationAction(): Flow<MainViewState> {
        val state = viewStateFlow.value

        val newState = state.copy(
            previousDrawnPaths = state.drawnPaths.map { it.copy(alpha = PREVIOUS_FRAME_ALPHA) },
            drawnPaths = emptyList()
        ) // TODO("Think about it")

        frameList.add(newState)

        return flowOf(frameList.last())
    }

    private fun processPlayAction(): Flow<MainViewState> {
        var index = 0
        return flow {
            while (index <= frameList.lastIndex) {
                val state = frameList[index]
                emit(state.copy(previousDrawnPaths = emptyList(), isAnimating = true))
                delay(FRAME_DELAY)
                if (index == frameList.lastIndex) index = 0 else index++
            }
        }
    }

    private fun processStopAction(): Flow<MainViewState> {
        return lastEditingState
    }

    private suspend fun processColorAction(@ColorInt color: Int, tool: ToolsState): Flow<MainViewState> {
        toolsStateFlow.emit(tool)
        val oldState = viewStateFlow.value
        val newState = if (color != Color.TRANSPARENT) oldState.copy(color = color) else oldState
        return flowOf(newState)
    }

    fun onDrawingTouched(event: MotionEvent) {
        userActionFlow.tryEmit(UserAction.DrawingAction(event))
    }

    fun onUndoClicked() {
        userActionFlow.tryEmit(UserAction.UndoAction)
    }

    fun onRestoreClicked() {
        userActionFlow.tryEmit(UserAction.RestoreAction)
    }

    fun onDeleteFrameClicked() {
        userActionFlow.tryEmit(UserAction.FrameDeletionAction)
    }

    fun onCreateFrameClicked() {
        userActionFlow.tryEmit(UserAction.FrameCreationAction)
    }

    fun onPlayClicked() {
        userActionFlow.tryEmit(UserAction.PlayAction)
    }

    fun onStopClicked() {
        userActionFlow.tryEmit(UserAction.StopAction)
    }

    fun onToolsClicked(tool: ToolsState) {
        toolsStateFlow.tryEmit(tool)
    }

    fun onColorClicked(tool: ToolsState, @ColorInt color: Int) {
        userActionFlow.tryEmit(UserAction.ColorAction(tool, color))
    }

    fun getDisplayFrames(context: Context): List<String> {
        return frameList.indices.map { position -> context.getString(R.string.frame_name, position + 1) }
    }
}