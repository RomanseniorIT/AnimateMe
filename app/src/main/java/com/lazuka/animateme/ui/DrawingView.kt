package com.lazuka.animateme.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.lazuka.animateme.ui.model.DrawnPath
import com.lazuka.animateme.ui.model.DrawingViewState

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
        drawnPathList.forEach { (path, color, alpha) ->
            brush.color = color
            brush.alpha = alpha
            canvas.drawPath(path, brush)
        }
    }

    fun setState(state: DrawingViewState) {
        when (state) {
            is DrawingViewState.Editing -> {
                drawnPathList.clear()
                drawnPathList.addAll(state.frame.drawnPaths)
                drawnPathList.addAll(state.frame.previousDrawnPaths)
            }

            is DrawingViewState.Display -> {
                drawnPathList.clear()
                drawnPathList.addAll(state.drawnPaths)
            }
        }
        invalidate()
    }
}