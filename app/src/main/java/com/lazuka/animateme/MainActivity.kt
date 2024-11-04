package com.lazuka.animateme

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.lazuka.animateme.databinding.ActivityMainBinding
import com.lazuka.animateme.databinding.PopupColorListBinding
import com.lazuka.animateme.databinding.PopupFigureListBinding
import com.lazuka.animateme.databinding.PopupFrameListBinding
import com.lazuka.animateme.popup.FigureListPopupWindow
import com.lazuka.animateme.popup.frame_list.FrameListPopupWindow
import com.lazuka.animateme.model.MainViewState
import com.lazuka.animateme.model.ToolsState
import com.lazuka.animateme.popup.ColorListPopupWindow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels(
        extrasProducer = {
            MutableCreationExtras().apply {
                val initialColor = ContextCompat.getColor(this@MainActivity, R.color.blue)
                val frameString = getString(R.string.frame_name)
                set(MainViewModel.INITIAL_COLOR_KEY, initialColor)
                set(MainViewModel.FRAME_STRING_KEY, frameString)
            }
        },
        factoryProducer = { MainViewModel.Factory }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        setContentView(binding.root)

        initViews()
        observeViewModel()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() = with(binding) {
        drawingView.setOnTouchListener { _, event ->
            viewModel.onDrawingTouched(event)
            return@setOnTouchListener true
        }

        ivUndo.setOnClickListener {
            viewModel.onUndoClicked()
        }

        ivRestore.setOnClickListener {
            viewModel.onRestoreClicked()
        }

        ivDeleteAll.setOnClickListener {
            viewModel.onDeleteAllFramesClicked()
        }

        ivDeleteFrame.setOnClickListener {
            viewModel.onDeleteFrameClicked()
        }

        ivCreateFrame.setOnClickListener {
            viewModel.onCreateFrameClicked()
        }

        ivCopyFrame.setOnClickListener {
            viewModel.onCopyFrameClicked()
        }

        ivFrameList.setOnClickListener {
            viewModel.onShowFrameListClicked()
        }

        ivPlay.setOnClickListener {
            viewModel.onPlayClicked()
        }

        ivStop.setOnClickListener {
            viewModel.onStopClicked()
        }

        rgTabBar.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                rbPencil.id -> viewModel.onToolsClicked(ToolsState.PENCIL)
                rbEraser.id -> viewModel.onToolsClicked(ToolsState.ERASER)
                rbTools.id -> viewModel.onToolsClicked(ToolsState.TOOLS)
                rbColor.id -> viewModel.onToolsClicked(ToolsState.COLORS)
            }
        }
    }

    private fun observeViewModel() = with(viewModel) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewStateFlow.collectLatest(::setState)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                showFrameListFlow.collectLatest(::showFrameListPopup)
            }
        }


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                clearToolsFlow.collectLatest { binding.rbPencil.isChecked = true }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                showToolsPopupFlow.collectLatest { showFigureListPopup() }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                toolsButtonFlow.collectLatest(::setToolsButton)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                showColorsPopupFlow.collectLatest { showColorListPopup() }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                colorsButtonFlow.collectLatest(::setColorsButton)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                loadingFlow.collectLatest { loading -> binding.progressBar.isVisible = loading}
            }
        }
    }

    private fun setState(state: MainViewState) = with(binding) {
        previousDrawingView.setDrawnPaths(state.previousDrawnPaths)
        drawingView.setDrawnPaths(state.drawnPaths)
        groupEditingButtons.isInvisible = state.isAnimating
        ivUndo.isEnabled = state.isUndoEnabled
        ivRestore.isEnabled = state.isRestoreEnabled
        ivStop.isEnabled = state.isStopEnabled
        ivPlay.isEnabled = state.isPlayEnabled
    }

    private fun showFrameListPopup(items: List<String>) {
        val binding = PopupFrameListBinding.inflate(layoutInflater)
        val popup = FrameListPopupWindow(binding, items)
        val margin = resources.getDimensionPixelSize(R.dimen.space_double)
        popup.showAsDropDown(this.binding.ivFrameList, 0, margin)
    }

    private fun showFigureListPopup() {
        val binding = PopupFigureListBinding.inflate(layoutInflater)
        val popup = FigureListPopupWindow(binding, viewModel::onToolsClicked)

        binding.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        popup.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popup.height = View.MeasureSpec.makeMeasureSpec(binding.root.measuredHeight, View.MeasureSpec.UNSPECIFIED)
        popup.showAsDropDown(
            this.binding.rbTools,
            0,
            (-0.2 * this.binding.rbTools.height).roundToInt(),
            Gravity.CENTER
        )
    }

    private fun showColorListPopup() {
        val binding = PopupColorListBinding.inflate(layoutInflater)
        val popup = ColorListPopupWindow(binding, viewModel::onColorClicked)

        binding.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        popup.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popup.height = View.MeasureSpec.makeMeasureSpec(binding.root.measuredHeight, View.MeasureSpec.UNSPECIFIED)
        popup.showAsDropDown(
            this.binding.rbColor,
            0,
            (-0.2 * this.binding.rbColor.height).roundToInt(),
            Gravity.CENTER
        )
    }

    private fun setToolsButton(tool: ToolsState) {
        if (tool == ToolsState.TOOLS) return
        val toolRes = when (tool) {
            ToolsState.CIRCLE -> R.drawable.ic_circle
            ToolsState.RECTANGLE -> R.drawable.ic_rectangle
            ToolsState.LINE -> R.drawable.ic_line
            else -> R.drawable.ic_tools
        }
        binding.rbTools.buttonDrawable = ContextCompat.getDrawable(this, toolRes)
    }

    private fun setColorsButton(tool: ToolsState) {
        when (tool) {
            ToolsState.WHITE -> {
                binding.rbColor.background = ContextCompat.getDrawable(this, R.drawable.ic_white_color)
            }

            ToolsState.RED -> {
                binding.rbColor.background = ContextCompat.getDrawable(this, R.drawable.ic_red_color)
            }

            ToolsState.BLACK -> {
                binding.rbColor.background = ContextCompat.getDrawable(this, R.drawable.ic_stroked_black_color)
            }

            ToolsState.BLUE -> {
                binding.rbColor.background = ContextCompat.getDrawable(this, R.drawable.ic_blue_color)
            }

            else -> Unit
        }

        binding.rbPencil.isChecked = true
    }
}