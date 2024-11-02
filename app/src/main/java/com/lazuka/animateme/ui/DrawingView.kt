package com.lazuka.animateme.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var brush = Paint()
    private var currentColor = Color.BLACK

    private var path = Path()
    private val pairList = mutableListOf<Pair<Path, Int>>()

    init {
        brush.isAntiAlias = true
        brush.color = Color.BLACK
        brush.style = Paint.Style.STROKE
        brush.strokeJoin = Paint.Join.ROUND
        brush.strokeWidth = 8f
    }

    val pathList = mutableListOf<Path>()
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) return false

        var x = event.x
        var y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                path.moveTo(x, y)
                pairList.add(path to currentColor)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                pathList.add(path)
                pairList.add(pairList.lastIndex, Path(path) to currentColor)
            }

            else -> return false
        }

        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        pairList.forEach { (path, color) ->
            brush.setColor(color)
            canvas.drawPath(path, brush)
        }
    }
}