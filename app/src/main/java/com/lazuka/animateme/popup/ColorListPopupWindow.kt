package com.lazuka.animateme.popup

import android.graphics.Color
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.lazuka.animateme.R
import com.lazuka.animateme.databinding.PopupColorListBinding
import com.lazuka.animateme.model.ToolsState

class ColorListPopupWindow(
    binding: PopupColorListBinding,
    private val onColorClicked: (ToolsState, Int) -> Unit
) : PopupWindow(
    binding.root,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    true
) {

    private var selectedColorTool = ToolsState.CLEARED
    private var selectedColor = Color.TRANSPARENT

    init {
        with(binding) {
            ivWhite.setOnClickListener {
                selectedColorTool = ToolsState.WHITE
                selectedColor = ContextCompat.getColor(root.context, R.color.white)
                dismiss()
            }

            ivRed.setOnClickListener {
                selectedColorTool = ToolsState.RED
                selectedColor = ContextCompat.getColor(root.context, R.color.red)
                dismiss()
            }

            ivBlack.setOnClickListener {
                selectedColorTool = ToolsState.BLACK
                selectedColor = ContextCompat.getColor(root.context, R.color.black)
                dismiss()
            }

            ivBlue.setOnClickListener {
                selectedColorTool = ToolsState.BLUE
                selectedColor = ContextCompat.getColor(root.context, R.color.blue)
                dismiss()
            }
        }
    }

    override fun dismiss() {
        super.dismiss()
        onColorClicked(selectedColorTool, selectedColor)
    }
}