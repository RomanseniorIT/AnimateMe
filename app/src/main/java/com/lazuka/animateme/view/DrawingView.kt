package com.lazuka.animateme.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.lazuka.animateme.model.DrawnPath

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val brush = Paint()
    private val drawnPathList = mutableListOf<DrawnPath>()

    init {
        brush.isAntiAlias = true
        brush.isDither = true
        brush.style = Paint.Style.STROKE
        brush.strokeJoin = Paint.Join.ROUND
        brush.strokeCap = Paint.Cap.ROUND
        brush.strokeWidth = 16f
    }

    override fun onDraw(canvas: Canvas) {
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        drawnPathList.forEach { (path, color, alpha, mode) ->
            brush.color = color
            brush.alpha = alpha
            brush.setXfermode(mode)
            canvas.drawPath(path, brush)
        }

        canvas.restoreToCount(layerId)
    }

    fun setDrawnPaths(drawnPath: List<DrawnPath>) {
        drawnPathList.clear()
        drawnPathList.addAll(drawnPath)
        invalidate()
    }
}