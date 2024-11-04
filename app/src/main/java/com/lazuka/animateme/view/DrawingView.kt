package com.lazuka.animateme.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.lazuka.animateme.R
import com.lazuka.animateme.model.DrawnPath

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val brush = Paint()
    private val drawnPathList = mutableListOf<DrawnPath>()

    private val cutBrush = Paint()
    private val cornerRadius = context.resources.getDimension(R.dimen.drawing_view_radius)
    private val cutBrushSize = context.resources.getDimension(R.dimen.cut_brush_size)
    private val cutRect = RectF()

    init {
        brush.isAntiAlias = true
        brush.isDither = true
        brush.style = Paint.Style.STROKE
        brush.strokeJoin = Paint.Join.ROUND
        brush.strokeCap = Paint.Cap.ROUND
        brush.strokeWidth = 16f

        cutBrush.isAntiAlias = true
        cutBrush.isDither = true
        cutBrush.style = Paint.Style.STROKE
        cutBrush.strokeWidth = cutBrushSize
        cutBrush.color = Color.BLACK
    }

    override fun onDraw(canvas: Canvas) {
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        drawnPathList.forEach { (path, color, alpha, mode) ->
            brush.color = color
            brush.alpha = alpha
            brush.setXfermode(mode)
            canvas.drawPath(path, brush)
        }

        drawRoundedCorners(canvas)

        canvas.restoreToCount(layerId)
    }

    private fun drawRoundedCorners(canvas: Canvas) {
        cutRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(cutRect, cornerRadius, cornerRadius, cutBrush)
    }

    fun setDrawnPaths(drawnPath: List<DrawnPath>) {
        drawnPathList.clear()
        drawnPathList.addAll(drawnPath)
        invalidate()
    }
}