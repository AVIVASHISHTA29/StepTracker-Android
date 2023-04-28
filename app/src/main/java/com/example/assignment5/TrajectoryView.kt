package com.example.assignment5

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.absoluteValue
import android.view.MotionEvent
import android.view.ScaleGestureDetector


class TrajectoryView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint()
    val path = Path()
    private var minWidth = 0f
    private var minHeight = 0f
    private var scaleFactor = 1f
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initialize(w / 2f, h / 2f)
    }

    fun initialize(startX: Float, startY: Float) {
        path.moveTo(startX, startY)
        invalidate()
    }

    init {
        paint.color = Color.BLACK
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE
    }

    fun addPoint(deltaX: Float, deltaY: Float) {
        path.rLineTo(deltaX, deltaY)
        minWidth += deltaX.absoluteValue
        minHeight += deltaY.absoluteValue

        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(minWidth.toInt(), widthMeasureSpec)
        val height = resolveSize(minHeight.toInt(), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceAtLeast(0.1f).coerceAtMost(5.0f)
            invalidate()
            return true
        }
    }
}
