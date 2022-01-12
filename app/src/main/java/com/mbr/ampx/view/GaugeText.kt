package com.mbr.ampx.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface

class GaugeText(color: Int, height: Float, bold: Boolean) {

    private var paint: Paint
    private lateinit var text: String
    private var x: Float = 0f
    private var y: Float = 0f
    var visible: Boolean = false

    init {
        paint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.textSize = height
        paint.textAlign = Paint.Align.CENTER
        paint.isElegantTextHeight = true
        if (bold) {
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))
        } else {
            paint.setTypeface(Typeface.SANS_SERIF)
        }
        visible = false
    }

    fun setText(text: String) { this.text = text }

    fun updatePosition(x: Float, y: Float, bounds: Rect) {
        paint.getTextBounds(text, 0, text.length, bounds)
        val textHeight = Math.abs(bounds.top + bounds.bottom) / 2f
        this.x = x
        this.y = y + textHeight
    }

    fun draw(canvas: Canvas) {
        if (visible) {
            canvas.drawText(text, x, y, paint)
        }
    }
}