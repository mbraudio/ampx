package com.mbr.ampx.view

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.mbr.ampx.R

class ModernSeekBar : View, GestureDetector.OnGestureListener {

    companion object {
        private const val DEFAULT_LINE_WIDTH = 56.0f
    }

    private var centerX = 0f
    private var startX = 0f
    private var endX = 0f
    private var centerY = 0f
    private var startY = 0f
    private var endY = 0f
    private var fromCenterX = 0f
    private lateinit var bounds: RectF

    private var width = 0f

    private lateinit var paintRect: Paint
    private lateinit var paintUnderlayLine: Paint
    private lateinit var paintLine: Paint

    // Gestures and Touches
    private lateinit var gestureDetector: GestureDetector
    private var listener: IListener? = null
    fun setListener(listener: IListener) { this.listener = listener }

    constructor(context: Context) : super(context) {
        initialize(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initialize(context, attrs, defStyle)
    }

    private fun initialize(context: Context, attrs: AttributeSet?, defStyle: Int) {
        loadAttributes(context, attrs, defStyle)

        gestureDetector = GestureDetector(context, this)
        bounds = RectF(0f, 0f, 0f, 0f)

        paintRect = Paint(Paint.ANTI_ALIAS_FLAG)
        paintRect.style = Paint.Style.STROKE
        paintRect.color = context.getColor(android.R.color.white)
        paintRect.strokeWidth = 2f

        paintUnderlayLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintUnderlayLine.style = Paint.Style.STROKE
        paintUnderlayLine.color = context.getColor(R.color.colorUnderlayArc)
        paintUnderlayLine.strokeWidth = width
        paintUnderlayLine.strokeCap = Paint.Cap.ROUND

        paintLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintLine.style = Paint.Style.STROKE
        paintLine.color = context.getColor(R.color.colorGradientEnd)
        paintLine.strokeWidth = width
        paintLine.strokeCap = Paint.Cap.ROUND
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.ModernSeekBar, defStyle, 0)

        width = a.getDimension(R.styleable.ModernSeekBar_modernGaugeWidth, DEFAULT_LINE_WIDTH)

        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minimumWidth = paddingStart + paddingEnd
        val w = resolveSizeAndState(minimumWidth, widthMeasureSpec, 1)
        val minimumHeight = paddingBottom + paddingTop
        val h = resolveSizeAndState(minimumHeight, heightMeasureSpec, 1)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        bounds.left = paddingLeft.toFloat()
        bounds.top = paddingTop.toFloat()
        bounds.bottom = h.toFloat() - paddingBottom
        bounds.right = w.toFloat() - paddingEnd
        centerX = (bounds.left + bounds.right) / 2f
        centerY = (bounds.top + bounds.bottom) / 2f

        startX = bounds.left + (width / 2f)
        endX = bounds.right - (width / 2f)

        startY = centerY
        endY = centerY

        fromCenterX = 0.25f
    }

    override fun onDraw(canvas: Canvas) {

        canvas.drawRect(bounds, paintRect)

        canvas.drawLine(startX, startY, endX, endY, paintUnderlayLine)
        canvas.drawLine(centerX - fromCenterX, startY, centerX, endY, paintLine)

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        var x = event.x
        if (x < startY) { x = startX }
        if (x > endX) { x = endX }

        fromCenterX = centerX - x

        if (event.action == MotionEvent.ACTION_UP) {
            val target = updateTargetValue()
            Log.e("MSB", "TARGET: $target")
            listener?.onValueSelection(target)
        }

        // Add a bit so value line always draw in the middle a bit
        if (fromCenterX == 0f) {
            fromCenterX = 0.25f
        }

        invalidate()
        return true
    }

    private fun updateTargetValue(): Int {
        return ((fromCenterX * 100f) / (centerX - startX)).toInt()
    }

    // Gesture Detector Listener
    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {

    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {

    }

    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        //toggleActive()
        //listener?.onSingleTapUp(active)
        return false
    }

    // Listener
    interface IListener {
        fun onValueSelection(value: Int)
    }
}