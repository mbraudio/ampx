package com.mbr.ampx.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.mbr.ampx.R

class ModernSeekBar : View, GestureDetector.OnGestureListener {

    companion object {
        private const val DEFAULT_LINE_WIDTH = 40.0f
        private const val DEFAULT_MAX_VALUE = 255.0f
        private const val DEFAULT_TEXT_SIZE = 24.0f
        private const val DEFAULT_TEXT_DISTANCE = 24.0f
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
    private var textSize = 0f
    private var textHeight = 0
    private var textDistance = 0f

    private var textTitle = ModernText()
    private var textStart = ModernText()
    private var textEnd = ModernText()

    private lateinit var paintRect: Paint
    private lateinit var paintUnderlayLine: Paint
    private lateinit var paintValueLine: Paint
    private lateinit var paintTargetLine: Paint
    private lateinit var textPaint: Paint

    // Gestures and Touches
    private var ignoreTouches = false
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
        gestureDetector.setIsLongpressEnabled(true)

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

        textPaint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
        textPaint.color = context.getColor(R.color.colorText)
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isElegantTextHeight = true
        textPaint.typeface = Typeface.DEFAULT
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.ModernSeekBar, defStyle, 0)
        width = a.getDimension(R.styleable.ModernSeekBar_modernGaugeWidth, DEFAULT_LINE_WIDTH)
        maximumValue = a.getFloat(R.styleable.ModernSeekBar_modernMaxValue, DEFAULT_MAX_VALUE)
        textSize = a.getDimension(R.styleable.ModernSeekBar_modernTextSize, DEFAULT_TEXT_SIZE)
        textTitle.text = a.getString(R.styleable.ModernSeekBar_modernTextTitle) ?: ""
        textStart.text = a.getString(R.styleable.ModernSeekBar_modernTextStart) ?: ""
        textEnd.text = a.getString(R.styleable.ModernSeekBar_modernTextEnd) ?: ""
        textDistance = a.getDimension(R.styleable.ModernSeekBar_modernTextDistance, DEFAULT_TEXT_DISTANCE)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minimumWidth = paddingStart + paddingEnd
        val w = resolveSizeAndState(minimumWidth, widthMeasureSpec, 1)
        val minimumHeight = paddingBottom + paddingTop + (textHeight * 2)
        val h = resolveSizeAndState(minimumHeight, heightMeasureSpec, 1)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateDimensionValues(w, h)
        calculateTextValues()
    }

    private fun calculateDimensionValues(w: Int, h: Int) {
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

    private fun calculateTextValues() {
        val text = "X"
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        textHeight = Math.abs(textBounds.top + textBounds.bottom)

        var w = textPaint.measureText(textTitle.text)
        var x = (centerX - (w / 2))
        var y = centerY - textHeight - textDistance
        textTitle.update(x, y)

        x = startX
        y = centerY + (textHeight * 2) + textDistance
        textStart.update(x, y)

        w = textPaint.measureText(textEnd.text)
        x = endX - w
        y = centerY + (textHeight * 2) + textDistance
        textEnd.update(x, y)
    }

    override fun onDraw(canvas: Canvas) {
        //canvas.drawRect(bounds, paintRect)
        canvas.drawLine(startX, startY, endX, endY, paintUnderlayLine)
        canvas.drawLine(centerX, startY, valueX, endY, paintValueLine)
        canvas.drawLine(valueX, startY, targetX, endY, paintTargetLine)

        textTitle.draw(canvas, textPaint)
        textStart.draw(canvas, textPaint)
        textEnd.draw(canvas, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        if (ignoreTouches) {
            return false
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
        ignoreTouches = false
        return true
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        ignoreTouches = true
        listener?.onLongPress((maximumValue / 2f).toInt(), this)
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {

    }

    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        return false
    }

    // Listener
    interface IListener {
        fun onValueSelection(value: Int, seekBar: ModernSeekBar)
        fun onLongPress(value: Int, seekBar: ModernSeekBar)
    }
}