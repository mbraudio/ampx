package com.mbr.ampx.view

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
        private const val DEFAULT_LINE_WIDTH = 40.0f
        private const val DEFAULT_MAX_VALUE = 255.0f
    }

    private var centerX = 0f
    private var startX = 0f
    private var endX = 0f
    private var centerY = 0f
    private var startY = 0f
    private var endY = 0f
    private var targetX = 0f
    private var valueX = 0f
    private lateinit var bounds: RectF

    private var width = 0f
    private var maximumValue = 0f

    private lateinit var paintRect: Paint
    private lateinit var paintUnderlayLine: Paint
    private lateinit var paintValueLine: Paint
    private lateinit var paintTargetLine: Paint

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

        paintValueLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintValueLine.style = Paint.Style.STROKE
        paintValueLine.color = context.getColor(R.color.colorGradientStart)
        paintValueLine.strokeWidth = width
        paintValueLine.strokeCap = Paint.Cap.ROUND

        paintTargetLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintTargetLine.style = Paint.Style.STROKE
        paintTargetLine.color = context.getColor(R.color.colorTargetArc)
        paintTargetLine.strokeWidth = (width * 0.5f)
        paintTargetLine.strokeCap = Paint.Cap.ROUND
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.ModernSeekBar, defStyle, 0)
        width = a.getDimension(R.styleable.ModernSeekBar_modernGaugeWidth, DEFAULT_LINE_WIDTH)
        maximumValue = a.getFloat(R.styleable.ModernSeekBar_modernMaxValue, DEFAULT_MAX_VALUE)
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
        bounds.left = paddingStart.toFloat()
        bounds.top = paddingTop.toFloat()
        bounds.bottom = h.toFloat() - paddingBottom
        bounds.right = w.toFloat() - paddingEnd
        centerX = (bounds.left + bounds.right) / 2f
        centerY = (bounds.top + bounds.bottom) / 2f

        startX = bounds.left + (width / 2f)
        endX = bounds.right - (width / 2f)

        startY = centerY
        endY = centerY

        setCurrentValue(128, 0)
    }

    override fun onDraw(canvas: Canvas) {
        //canvas.drawRect(bounds, paintRect)
        canvas.drawLine(startX, startY, endX, endY, paintUnderlayLine)
        canvas.drawLine(centerX, startY, valueX, endY, paintValueLine)
        canvas.drawLine(valueX, startY, targetX, endY, paintTargetLine)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        targetX = event.x
        if (targetX < startY) { targetX = startX }
        if (targetX > endX) { targetX = endX }

        if (event.action == MotionEvent.ACTION_UP) {
            val target = updateTargetValue()
            listener?.onValueSelection(target, this)
        }

        invalidate()
        return true
    }

    private fun updateTargetValue(): Int {
        return (((targetX - startX) * maximumValue) / (endX - startX)).toInt()
    }

    fun setCurrentValue(current: Int, active: Int) {
        valueX = ((current * (endX - startX)) / maximumValue) + startX
        if (active == 0) {
            targetX = valueX
        }
        invalidate()
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
        fun onValueSelection(value: Int, seekBar: ModernSeekBar)
    }
}