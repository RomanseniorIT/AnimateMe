package com.lazuka.animateme

import android.graphics.Color
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lazuka.animateme.model.DrawnPath
import com.lazuka.animateme.model.MainViewState
import com.lazuka.animateme.model.ToolsState
import com.lazuka.animateme.model.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MainViewModel(
    @ColorInt private val initialColor: Int,
    private val frameString: String
) : ViewModel() {

    private val toolsStateFlow = MutableStateFlow(ToolsState.INITIAL)
    val clearToolsFlow = toolsStateFlow.filter { tool -> tool == ToolsState.INITIAL }
    val showToolsPopupFlow = toolsStateFlow.filter { tool -> tool == ToolsState.TOOLS }
    val toolsButtonFlow = toolsStateFlow.filter { tool -> tool != ToolsState.INITIAL }
    val showColorsPopupFlow = toolsStateFlow.filter { tool -> tool == ToolsState.COLORS }
    val colorsButtonFlow = toolsStateFlow.filter { tool ->
        tool == ToolsState.WHITE || tool == ToolsState.RED || tool == ToolsState.BLACK || tool == ToolsState.BLUE
    }

    private val frameList = mutableListOf(MainViewState(initialColor, emptyList()))

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
                is UserAction.FrameCopyAction -> processFrameCopyAction()
                is UserAction.AllFramesDeletion -> processAllFramesDeletionAction()
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

    private val onShowFrameListClicked = Channel<Unit>()
    val showFrameListFlow = onShowFrameListClicked.receiveAsFlow()
        .onEach { _loadingFlow.emit(true) }
        .map { getDisplayFrames() }
        .onEach { _loadingFlow.emit(false) }

    private val _loadingFlow = MutableStateFlow(false)
    val loadingFlow: StateFlow<Boolean> = _loadingFlow

    private fun processDrawingAction(action: UserAction.DrawingAction): Flow<MainViewState> {
        val toolsState = toolsStateFlow.value
        val mode = when (toolsState) {
            ToolsState.INITIAL, ToolsState.TOOLS -> return viewStateFlow
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
                flowOf(currentState.copy(drawnPaths = actions, isRestoreEnabled = false))
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

        return flowOf(state.copy(drawnPaths = drawnPath, isRestoreEnabled = true))
    }

    private fun processRestoreAction(): Flow<MainViewState> {
        return drawnPathHistory.map { pathHistory ->
            val state = viewStateFlow.value
            val drawnPath = state.drawnPaths.toMutableList()
            val restoredIndex = drawnPath.lastIndex + 1

            val canRestore = restoredIndex < pathHistory.size
            if (canRestore) {
                drawnPath.add(pathHistory[restoredIndex])
            }

            state.copy(drawnPaths = drawnPath, isRestoreEnabled = drawnPath.size < pathHistory.size)
        }
    }

    private fun processFrameDeletionAction(): Flow<MainViewState> {
        val oldState = viewStateFlow.value
        val newState = if (frameList.size == 1) {
            frameList[FIRST_FRAME_POSITION] = oldState.copy(drawnPaths = emptyList(), isPlayEnabled = false)
            frameList.first()
        } else {
            frameList.removeAt(frameList.lastIndex)
            frameList.last()
        }

        return flowOf(newState)
    }

    private suspend fun processFrameCreationAction(): Flow<MainViewState> {
        _loadingFlow.emit(true)

        val state = viewStateFlow.value
        return withContext(Dispatchers.IO) {
            val newState = state.copy(
                previousDrawnPaths = state.drawnPaths.map { it.copy(alpha = PREVIOUS_FRAME_ALPHA) },
                drawnPaths = emptyList(),
                isPlayEnabled = true
            )

            frameList.add(newState)

            flowOf(frameList.last()).onEach { _loadingFlow.emit(false) }
        }
    }

    private fun processPlayAction(): Flow<MainViewState> {
        var index = 0
        return flow {
            while (index <= frameList.lastIndex) {
                val state = frameList[index]
                emit(
                    state.copy(
                        previousDrawnPaths = emptyList(),
                        isAnimating = true,
                        isPlayEnabled = false,
                        isStopEnabled = true
                    )
                )
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

    private suspend fun processFrameCopyAction(): Flow<MainViewState> {
        _loadingFlow.emit(true)

        val state = viewStateFlow.value
        return withContext(Dispatchers.IO) {
            val newState = state.copy(
                previousDrawnPaths = state.drawnPaths.map { it.copy(alpha = PREVIOUS_FRAME_ALPHA) },
                isPlayEnabled = true
            )

            frameList.add(newState)

            flowOf(frameList.last()).onEach { _loadingFlow.emit(false) }
        }
    }

    private fun processAllFramesDeletionAction(): Flow<MainViewState> {
        val oldState = viewStateFlow.value
        frameList.clear()
        frameList.add(oldState.copy(previousDrawnPaths = emptyList(), drawnPaths = emptyList(), isPlayEnabled = false))

        return flowOf(frameList.first())
    }

    private suspend fun getDisplayFrames(): List<String> = withContext(Dispatchers.IO) {
        frameList.indices.map { position -> String.format(frameString, position + 1) }
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

    fun onCopyFrameClicked() {
        userActionFlow.tryEmit(UserAction.FrameCopyAction)
    }

    fun onDeleteAllFramesClicked() {
        userActionFlow.tryEmit(UserAction.AllFramesDeletion)
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

    fun onShowFrameListClicked() {
        onShowFrameListClicked.trySend(Unit)
    }

    companion object {
        private const val FIRST_FRAME_POSITION = 0
        private const val AVAILABLE_TOUCHES_AMOUNT = 1
        private const val PREVIOUS_FRAME_ALPHA = 100
        private const val FRAME_DELAY = 500L

        val INITIAL_COLOR_KEY = object : CreationExtras.Key<Int> {}
        val FRAME_STRING_KEY = object : CreationExtras.Key<String> {}
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val initialColor = this[INITIAL_COLOR_KEY] as Int
                val frameString = this[FRAME_STRING_KEY] as String
                MainViewModel(
                    initialColor = initialColor,
                    frameString = frameString
                )
            }
        }
    }
}