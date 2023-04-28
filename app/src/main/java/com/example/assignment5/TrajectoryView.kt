package com.example.assignment5

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.absoluteValue

class TrajectoryView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    val path = Path()

    private val paint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var prevX = 0f
    private var prevY = 0f

    fun initialize() {
        prevX = width / 2f
        prevY = height / 2f
        path.moveTo(prevX, prevY)
    }

    fun addPoint(deltaX: Float, deltaY: Float) {
        val newX = prevX + deltaX
        val newY = prevY - deltaY

        path.lineTo(newX, newY)
        prevX = newX
        prevY = newY

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }
}
