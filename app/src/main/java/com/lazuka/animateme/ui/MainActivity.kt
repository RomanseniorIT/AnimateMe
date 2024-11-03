package com.lazuka.animateme.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lazuka.animateme.R
import com.lazuka.animateme.databinding.ActivityMainBinding
import com.lazuka.animateme.databinding.PopupFrameListBinding
import com.lazuka.animateme.ui.frame_list.FrameListPopupWindow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

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

        ivDeleteFrame.setOnClickListener {
            viewModel.onDeleteFrameClicked()
        }

        ivCreateFrame.setOnClickListener {
            viewModel.onCreateFrameClicked()
        }

        ivFrameList.setOnClickListener {
            showFrameListPopup()
        }

        ivPlay.setOnClickListener {
            viewModel.onPlayClicked()
        }

        ivStop.setOnClickListener {
            viewModel.onStopClicked()
        }
    }

    private fun observeViewModel() = with(viewModel) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                drawingStateFlow.collectLatest(binding.drawingView::setState)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                editingButtonsInvisibleFlow.collectLatest { isInvisible ->
                    binding.groupEditingButtons.isInvisible = isInvisible
                }
            }
        }
    }

    private fun showFrameListPopup() {
        val binding = PopupFrameListBinding.inflate(layoutInflater)
        val items = viewModel.getDisplayFrames(this)
        val popup = FrameListPopupWindow(binding, items)
        val margin = resources.getDimensionPixelSize(R.dimen.space)
        popup.showAsDropDown(this.binding.ivFrameList, 0, margin)
    }
}