package com.lazuka.animateme.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.lazuka.animateme.ui.model.DrawnPath
import com.lazuka.animateme.ui.model.MainViewState

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val brush = Paint()
    private val drawnPathList = mutableListOf<DrawnPath>()

    init {
        brush.isAntiAlias = true
        brush.style = Paint.Style.STROKE
        brush.strokeJoin = Paint.Join.ROUND
        brush.strokeWidth = 8f
    }

    override fun onDraw(canvas: Canvas) {
        drawnPathList.forEach { (path, color) ->
            brush.setColor(color)
            canvas.drawPath(path, brush)
        }
    }

    fun setState(state: MainViewState) {
        when (state) {
            is MainViewState.Drawing -> {
                drawnPathList.clear()
                drawnPathList.addAll(state.frame.drawnPaths)
                invalidate()
            }

            is MainViewState.Animating -> {
                // TODO("Implement animating state")
            }
        }
    }
}