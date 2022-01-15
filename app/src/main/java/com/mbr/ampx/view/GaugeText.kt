package com.mbr.ampx.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface

class GaugeText(color: Int, height: Float, bold: Boolean) {

    var paint: Paint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
    var text: String = ""
    private var x: Float = 0f
    private var y: Float = 0f

    init {
        paint.color = color
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.textSize = height
        paint.textAlign = Paint.Align.CENTER
        paint.isElegantTextHeight = true
        if (bold) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } else {
            paint.typeface = Typeface.DEFAULT
        }
    }

    fun updatePosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun draw(canvas: Canvas) {
        canvas.drawText(text, x, y, paint)
    }
}