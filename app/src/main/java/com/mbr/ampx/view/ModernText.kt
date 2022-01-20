package com.mbr.ampx.view

import android.graphics.Canvas
import android.graphics.Paint

class ModernText {

    var text = ""
    var x = 0f
    var y = 0f

    fun update(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawText(text, x, y, paint)
    }

}