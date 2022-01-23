package com.mbr.ampx.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.mbr.ampx.R
import com.mbr.ampx.listener.IGaugeViewListener
import com.mbr.ampx.utilities.Constants
import kotlin.math.sqrt

class GaugeViewSimple : View, GestureDetector.OnGestureListener {

    companion object {
        //private val TAG = GaugeView::class.java.simpleName

        // DEFAULT VALUES
        private const val DEFAULT_MAXIMUM_VALUE = 255
        const val DEFAULT_MAXIMUM_VALUE_HALF = 127
        private const val DEFAULT_CENTER_VALUE_TEXT_HEIGHT = 50.0f

        // ADJUSTMENTS
        private const val VALUE_ARC_STROKE_WIDTH = 40.0f
        private const val COLORED_CIRCLE_WIDTH = 6.0f
    }

    // TYPED VARIABLES
    private val touchEnabled = true
    private val startAngle = 120f
    private val endAngle = 420f
    private val totalAngle = endAngle - startAngle
    private var centerAngle = (totalAngle / 2f) + startAngle
    private var valueAngle = 0f
    private var targetAngle = 0f
    private var touchDistanceMin = 0f
    private var touchDistanceMax = 0f
    private var maximumValue = 0
    private var radius = 0f

    // CENTER TEXT
    private var titleTextHeight = 0f
    private var valueTextHeight = 0f
    private var boldText = false

    private var bounds = RectF()
    private var centerX = 0f
    private var centerY = 0f
    private var touchX = 0f
    private var touchY = 0f

    // Value arc(s)
    private lateinit var boundsValueArc: RectF
    private lateinit var paintUnderlayArc: Paint
    private lateinit var paintValueArc: Paint
    private lateinit var paintTargetArc: Paint
    // Inner circles
    private lateinit var paintColoredCircle: Paint
    private var coloredCircleRadius: Float = 0f

    private lateinit var scaleTextPaint: Paint
    private var texts = ArrayList<GaugeText>()

    private var title = ""

    private lateinit var valueText: GaugeText
    private lateinit var titleText: GaugeText

    // Gestures and Touches
    private var ignoreTouches = false
    private lateinit var gestureDetector: GestureDetector
    private var listener: IGaugeViewListener? = null
    fun setListener(listener: IGaugeViewListener) { this.listener = listener }

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

        // Value arc(s)
        boundsValueArc = RectF(0f, 0f, 0f, 0f)
        paintUnderlayArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintUnderlayArc.style = Paint.Style.STROKE
        paintUnderlayArc.color = context.getColor(R.color.colorUnderlayArc)
        paintUnderlayArc.strokeWidth = VALUE_ARC_STROKE_WIDTH
        paintUnderlayArc.strokeCap = Paint.Cap.ROUND

        paintValueArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintValueArc.style = Paint.Style.STROKE
        paintValueArc.color = context.getColor(R.color.colorGradientEnd)
        paintValueArc.strokeWidth = VALUE_ARC_STROKE_WIDTH
        paintValueArc.strokeCap = Paint.Cap.ROUND

        paintTargetArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintTargetArc.style = Paint.Style.STROKE
        paintTargetArc.color = context.getColor(R.color.colorTargetArc)
        paintTargetArc.strokeWidth = VALUE_ARC_STROKE_WIDTH
        paintTargetArc.strokeCap = Paint.Cap.ROUND

        // Inner circles
        // Colored
        paintColoredCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintColoredCircle.style = Paint.Style.STROKE
        paintColoredCircle.color = context.getColor(R.color.colorGradientStart)
        paintColoredCircle.strokeWidth = COLORED_CIRCLE_WIDTH

        scaleTextPaint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
        scaleTextPaint.color = context.getColor(R.color.colorText)
        scaleTextPaint.style = Paint.Style.FILL_AND_STROKE
        scaleTextPaint.textSize = titleTextHeight
        scaleTextPaint.textAlign = Paint.Align.LEFT
        scaleTextPaint.isElegantTextHeight = true
        scaleTextPaint.typeface = Typeface.SERIF

        valueText = GaugeText(context.getColor(android.R.color.white), valueTextHeight, boldText)
        titleText = GaugeText(context.getColor(android.R.color.white), titleTextHeight, boldText)
        titleText.text = title
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.GaugeViewSimple, defStyle, 0)
        maximumValue = a.getInt(R.styleable.GaugeViewSimple_maximumValueSimple, DEFAULT_MAXIMUM_VALUE)
        titleTextHeight = a.getDimension(R.styleable.GaugeViewSimple_titleTextHeightSimple, DEFAULT_CENTER_VALUE_TEXT_HEIGHT)
        valueTextHeight = a.getDimension(R.styleable.GaugeViewSimple_valueTextHeightSimple, DEFAULT_CENTER_VALUE_TEXT_HEIGHT)
        boldText = a.getBoolean(R.styleable.GaugeViewSimple_valueTextBoldSimple, false)
        val text = a.getString(R.styleable.GaugeViewSimple_titleSimple)
        text?.let {
            title = it
        }
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minimumWidth = paddingStart + paddingEnd
        val w = resolveSizeAndState(minimumWidth, widthMeasureSpec, 1)
        val minimumHeight = paddingBottom + paddingTop
        val h = resolveSizeAndState(minimumHeight, heightMeasureSpec, 1)
        setMeasuredDimension(w, h)
    }

    private fun calculateDimensionValues(w: Int, h: Int) {
        bounds.left = paddingStart.toFloat()
        bounds.top = paddingTop.toFloat()
        bounds.bottom = h.toFloat() - paddingBottom
        bounds.right = w.toFloat() - paddingEnd
        centerX = (bounds.left + bounds.right) / 2f
        centerY = (bounds.top + bounds.bottom) / 2f
        radius = Math.min(bounds.right - bounds.left, bounds.bottom - bounds.top) / 2f

        // Value arc(s)
        val strokeHalf = VALUE_ARC_STROKE_WIDTH / 2f
        boundsValueArc.left = bounds.left + strokeHalf
        boundsValueArc.top = bounds.top + strokeHalf
        boundsValueArc.right = bounds.right - strokeHalf
        boundsValueArc.bottom = bounds.bottom - strokeHalf
        touchDistanceMin = radius - (VALUE_ARC_STROKE_WIDTH * 3f)
        touchDistanceMax = radius + (VALUE_ARC_STROKE_WIDTH * 3f)

        // Inner circles
         // Colored
        coloredCircleRadius = radius - (radius * 3f / 7f)

        // Value text
        valueText.text = "0"
        val textBounds = Rect()
        valueText.paint.getTextBounds(valueText.text, 0, valueText.text.length, textBounds)
        var textHeightHalf = Math.abs(textBounds.top + textBounds.bottom).toFloat()
        valueText.updatePosition(centerX, centerY + coloredCircleRadius + (textHeightHalf * 2f))

        // Title text
        titleText.paint.getTextBounds(titleText.text, 0, titleText.text.length, textBounds)
        textHeightHalf = Math.abs(textBounds.top + textBounds.bottom) / 2f
        titleText.updatePosition(centerX, centerY + textHeightHalf)
    }

    private fun addText() {
        // Add text for min and max
        addText(startAngle, context.getString(R.string.minus_sign))
        addText(endAngle, context.getString(R.string.plus_sign))
    }

    private fun addText(angle: Float, text: String) {
        val textBounds = Rect()
        val textOffsetX = 64f
        val angleRadians = Math.toRadians(angle.toDouble())
        val cosA = Math.cos(angleRadians)
        val sinA = Math.sin(angleRadians)
        scaleTextPaint.getTextBounds(text, 0, text.length, textBounds)
        val textHeight = Math.abs(textBounds.top + textBounds.bottom) / 2f
        val x = (centerX + (radius + textOffsetX) * cosA).toFloat()
        val y = (centerY + (radius) * sinA).toFloat() + (textHeight * 3f/2f)

        val gaugeText = GaugeText(context.getColor(android.R.color.white), valueTextHeight, false)
        gaugeText.text = text
        gaugeText.updatePosition(x, y)

        texts.add(gaugeText)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateDimensionValues(w, h)
        addText()
    }

    override fun onDraw(canvas: Canvas) {
        //canvas.drawRect(bounds, paintColoredCircle)
        canvas.drawArc(boundsValueArc, startAngle, totalAngle, false, paintUnderlayArc)
        canvas.drawArc(boundsValueArc, centerAngle, valueAngle, false, paintValueArc)
        canvas.drawArc(boundsValueArc, centerAngle + valueAngle, targetAngle - (centerAngle + valueAngle), false, paintTargetArc)

        canvas.drawCircle(centerX, centerY, coloredCircleRadius, paintColoredCircle)

        for (text in texts) {
            text.draw(canvas)
        }

        valueText.draw(canvas)
        titleText.draw(canvas)

        // Center spot
        //canvas.drawCircle(centerX, centerY, 1f, paintColoredCircle)
    }

    fun setCurrentValue(current: Int, active: Int) {
        // Current 0 - 255
        val value = current - DEFAULT_MAXIMUM_VALUE_HALF
        valueAngle = (value * totalAngle) / DEFAULT_MAXIMUM_VALUE
        if (active == 0) {
            targetAngle = centerAngle + valueAngle
        }
        valueText.text = "$value"
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (touchEnabled) {
            if (gestureDetector.onTouchEvent(event)) {
                return true
            }

            if (ignoreTouches) {
                return false
            }

            touchX = event.x
            touchY = event.y
            val d = distanceFromCenter(touchX, touchY)
            if (d > touchDistanceMin && d < touchDistanceMax) {
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        updateTargetAngle()
                    }
                    MotionEvent.ACTION_UP -> {
                        val value = calculateValue(targetAngle)
                        listener?.onGaugeViewValueSelection(value, maximumValue, id)
                    }
                }
                //Log.e(TAG, "ACTION: " + event.getAction());
                invalidate()
                return true
            }
            false
        } else {
            super.onTouchEvent(event)
        }
    }

    private fun distanceFromCenter(x: Float, y: Float): Float {
        val xd = Math.abs(x - centerX).toDouble()
        val yd = Math.abs(y - centerY).toDouble()
        return Math.sqrt(Math.pow(xd, 2.0) + Math.pow(yd, 2.0)).toFloat()
    }

    private fun updateTargetAngle() {
        // Calculate the angle of user touch
        val angle = getAngleForPoint(touchX, touchY)
        if ((angle < startAngle) || (angle > endAngle)) {
            return
        }
        targetAngle = angle
        calculateValue(angle)
    }

    private fun calculateValue(angle: Float): Int {
        val value = (((angle - startAngle) * maximumValue) / totalAngle).toInt()
        valueText.text = "$value"
        return value
    }

    private fun getAngleForPoint(x: Float, y: Float): Float {
        val tx = (x - centerX).toDouble()
        val ty = (y - centerY).toDouble()
        val length = sqrt(tx * tx + ty * ty)
        val r = Math.acos(ty / length)
        var angle = Math.toDegrees(r).toFloat()
        if (x > centerX) {
            angle = 360f - angle
        }
        angle += 90f
        return angle
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
        listener?.onGaugeViewLongPress(false, id)
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {

    }

    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        return false
    }
}