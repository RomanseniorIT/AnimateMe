package com.lazuka.animateme.popup

import android.widget.LinearLayout
import android.widget.PopupWindow
import com.lazuka.animateme.databinding.PopupFigureListBinding
import com.lazuka.animateme.model.ToolsState

class FigureListPopupWindow(
    binding: PopupFigureListBinding,
    private val onFigureClicked: (ToolsState) -> Unit
) : PopupWindow(
    binding.root,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    true
) {

    private var selectedTool = ToolsState.INITIAL

    init {
        with(binding) {
            ivRectangle.setOnClickListener {
                selectedTool = ToolsState.RECTANGLE
                dismiss()
            }

            ivCircle.setOnClickListener {
                selectedTool = ToolsState.CIRCLE
                dismiss()
            }

            ivLine.setOnClickListener {
                selectedTool = ToolsState.LINE
                dismiss()
            }
        }
    }

    override fun dismiss() {
        onFigureClicked(selectedTool)
        super.dismiss()
    }
}