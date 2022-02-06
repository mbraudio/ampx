package com.mbr.ampx.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.mbr.ampx.R
import com.mbr.ampx.listener.IGaugeViewListener
import com.mbr.ampx.utilities.Constants
import java.lang.Math.toDegrees
import kotlin.math.*

class GaugeViewEx : View, GestureDetector.OnGestureListener {

    companion object {
        //private val TAG = GaugeView::class.java.simpleName

        // DEFAULT VALUES
        private const val DEFAULT_NUMBER_OF_DIVISIONS = 50
        private const val DEFAULT_DIVISION_VALUE = 2
        private const val DEFAULT_MAXIMUM_VALUE = 100
        private const val DEFAULT_CENTER_VALUE_TEXT_HEIGHT = 60.0f

        // ADJUSTMENTS
        private const val VALUE_ARC_STROKE_WIDTH = 56.0f
        private const val SCALE_LINE_LENGTH = 40.0f
        private const val OUTER_ARC_DISTANCE_TO_VALUE_ARC = 16.0f
        private const val VALUE_ARC_DISTANCE_TO_SCALE = 24.0f
        private const val SCALE_ARC_DISTANCE_TO_COLORED_CIRCLE = 120.0f
        private const val COLORED_CIRCLE_WIDTH = 8.0f
        private const val SMALL_TEXT_HEIGHT = 40.0f
    }

    // Center image
    private lateinit var bitmapPaint: Paint
    private var bitmap: Bitmap? = null
    private var normalBitmap: Bitmap? = null
    private var activeBitmap: Bitmap? = null
    private var active = false
    private var bitmapX = 0f
    private var bitmapY = 0f

    // VARIABLES
    private val startAngle = 120f
    private val endAngle = 420f
    private val totalAngle = endAngle - startAngle
    private var valueAngle = 0f
    private var targetAngle = 0f
    private var touchDistanceMin = 0f
    private var touchDistanceMax = 0f
    private var numberOfDivisions = 0
    private var divisionValue = 0
    private var maximumValue = 0
    private var radius = 0f
    private var unit: String? = null

    private var scaleDiameter: Float = 0f

    // CENTER TEXT
    private var centerValueTextHeight = 0f
    private var valueTextBold = false

    private var bounds = RectF()
    private var centerX = 0f
    private var centerY = 0f
    private var touchX = 0f
    private var touchY = 0f

    private lateinit var paintSpot: Paint
    private var spotX = 0f
    private var spotY = 0f
    private var spotRadius = 0f
    private var drawSpot = false


    // Value arc(s)
    private lateinit var boundsValueArc: RectF
    private lateinit var paintUnderlayArc: Paint
    private lateinit var paintValueArc: Paint
    private lateinit var paintTargetArc: Paint
    // Scale lines
    private var lines = ArrayList<GaugeLine>()
    private lateinit var paintScaleLine: Paint
    // Inner circles
    private lateinit var paintColoredCircle: Paint
    private var coloredCircleRadius: Float = 0f

    private lateinit var scaleTextPaint: Paint

    private var texts = ArrayList<GaugeText>()

    private lateinit var valueText: GaugeText
    private lateinit var dacText: GaugeText

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
        paintValueArc.strokeWidth = VALUE_ARC_STROKE_WIDTH
        paintValueArc.strokeCap = Paint.Cap.ROUND

        paintTargetArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintTargetArc.style = Paint.Style.STROKE
        paintTargetArc.color = context.getColor(R.color.colorTargetArc)
        paintTargetArc.strokeWidth = VALUE_ARC_STROKE_WIDTH
        paintTargetArc.strokeCap = Paint.Cap.ROUND

        // Scale lines
        val scaleLineStrokeWidth = 4f
        paintScaleLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintScaleLine.style = Paint.Style.STROKE
        paintScaleLine.color = context.getColor(R.color.colorGradientEnd)
        paintScaleLine.strokeWidth = scaleLineStrokeWidth

        // Inner circles
        // Colored
        paintColoredCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintColoredCircle.style = Paint.Style.STROKE
        paintColoredCircle.color = context.getColor(R.color.colorGradientStart)
        paintColoredCircle.strokeWidth = COLORED_CIRCLE_WIDTH

        // Spot
        paintSpot = Paint(Paint.ANTI_ALIAS_FLAG)
        paintSpot.style = Paint.Style.FILL_AND_STROKE
        paintSpot.color = context.getColor(R.color.colorGradientStart)
        paintSpot.strokeWidth = COLORED_CIRCLE_WIDTH

        scaleTextPaint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
        scaleTextPaint.color = context.getColor(R.color.colorText)
        scaleTextPaint.style = Paint.Style.FILL_AND_STROKE
        scaleTextPaint.textSize = 38.0f
        scaleTextPaint.textAlign = Paint.Align.LEFT
        scaleTextPaint.isElegantTextHeight = true
        scaleTextPaint.typeface = Typeface.SERIF //ResourcesCompat.getFont(context, R.font.orbitron)

        valueText = GaugeText(context.getColor(android.R.color.white), centerValueTextHeight, valueTextBold)
        dacText = GaugeText(context.getColor(android.R.color.white), centerValueTextHeight / 2f, false)

        dacText.text = "48kHz @ 24bit"

        bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bitmapPaint.style = Paint.Style.FILL

        adjustActive()
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.GaugeViewEx, defStyle, 0)
        numberOfDivisions = a.getInt(R.styleable.GaugeViewEx_numberOfDivisionsEx, DEFAULT_NUMBER_OF_DIVISIONS)
        divisionValue = a.getInt(R.styleable.GaugeViewEx_divisionValueEx, DEFAULT_DIVISION_VALUE)
        maximumValue = a.getInt(R.styleable.GaugeViewEx_maximumValueEx, DEFAULT_MAXIMUM_VALUE)
        centerValueTextHeight = a.getDimension(R.styleable.GaugeViewEx_valueTextHeightEx, DEFAULT_CENTER_VALUE_TEXT_HEIGHT)
        unit = a.getString(R.styleable.GaugeViewEx_unitEx)
        if (unit == null) {
            unit = resources.getString(R.string.percentage)
        }
        valueTextBold = a.getBoolean(R.styleable.GaugeViewEx_valueTextBoldEx, false)
        active = a.getBoolean(R.styleable.GaugeViewEx_gaugeActive, false)
        var image = a.getResourceId(R.styleable.GaugeViewEx_gaugeNormalImage, 0)
        if (image != 0) {
            normalBitmap = BitmapFactory.decodeResource(resources, image)
        }
        image = a.getResourceId(R.styleable.GaugeViewEx_gaugeActiveImage, 0)
        if (image != 0) {
            activeBitmap = BitmapFactory.decodeResource(resources, image)
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
        radius = min(bounds.right - bounds.left, bounds.bottom - bounds.top) / 2f

        spotRadius = radius / 40f

        val positions = floatArrayOf(0.0f, 1.0f)
        val colors = intArrayOf(ContextCompat.getColor(context, R.color.colorGradientStart), ContextCompat.getColor(context, R.color.colorGradientEnd))
        val gradient = SweepGradient(centerX, centerY, colors, positions)
        val matrix = Matrix()
        matrix.preRotate(startAngle - (VALUE_ARC_STROKE_WIDTH / 2f), centerX, centerY)
        gradient.setLocalMatrix(matrix)
        paintValueArc.shader = gradient

        // Value arc(s)
        val strokeHalf = VALUE_ARC_STROKE_WIDTH / 2f
        boundsValueArc.left = bounds.left + strokeHalf + OUTER_ARC_DISTANCE_TO_VALUE_ARC
        boundsValueArc.top = bounds.top + strokeHalf + OUTER_ARC_DISTANCE_TO_VALUE_ARC
        boundsValueArc.right = bounds.right - strokeHalf - OUTER_ARC_DISTANCE_TO_VALUE_ARC
        boundsValueArc.bottom = bounds.bottom - strokeHalf - OUTER_ARC_DISTANCE_TO_VALUE_ARC
        val diameterScaleArc = radius - OUTER_ARC_DISTANCE_TO_VALUE_ARC
        touchDistanceMin = diameterScaleArc - VALUE_ARC_STROKE_WIDTH * 4f
        touchDistanceMax = diameterScaleArc + VALUE_ARC_STROKE_WIDTH * 3f
        scaleDiameter = diameterScaleArc - VALUE_ARC_STROKE_WIDTH - VALUE_ARC_DISTANCE_TO_SCALE

        // Inner circles
         // Colored
        coloredCircleRadius = scaleDiameter - SCALE_LINE_LENGTH - SCALE_ARC_DISTANCE_TO_COLORED_CIRCLE

        bitmap = BitmapFactory.decodeResource(resources, R.drawable.round_volume_up_white_36)

        var distanceY = radius / 28f
        if (distanceY < 12f) {
            distanceY = 12f
        }

        // Value text
        adjustGaugeText(valueText, distanceY)
        // Dac texts
        val radiusHalf = radius / 2f
        adjustGaugeText(dacText, radiusHalf + (centerValueTextHeight / 2f) + 6f)

        bitmap?.let {
            bitmapX = centerX - (it.width / 2f)
            bitmapY = centerY - it.height - distanceY
        }
    }

    private fun adjustGaugeText(gaugeText: GaugeText, distanceY: Float) {
        val textBounds = Rect()
        gaugeText.paint.getTextBounds(gaugeText.text, 0, gaugeText.text.length, textBounds)
        val textHeight = abs(textBounds.top + textBounds.bottom) / 2f
        gaugeText.updatePosition(centerX, centerY + (textHeight * 2f) + distanceY)
    }

    private fun calculateScaleValues() {
        lines.clear()
        val numberOfLines = numberOfDivisions + 1
        lines = ArrayList(numberOfLines)
        val totalAngle = endAngle - startAngle
        val divisionAngle = totalAngle / numberOfDivisions
        val lineStart = scaleDiameter
        val lineEnd = scaleDiameter - SCALE_LINE_LENGTH
        for (i in 0 until numberOfLines) {
            val currentAngle = (startAngle + i * divisionAngle).toDouble()
            val angleRadians = Math.toRadians(currentAngle)
            val cosA = cos(angleRadians)
            val sinA = sin(angleRadians)
            val startX = (centerX + lineStart * cosA).toFloat()
            val startY = (centerY + lineStart * sinA).toFloat()
            val stopX = (centerX + lineEnd * cosA).toFloat()
            val stopY = (centerY + lineEnd * sinA).toFloat()
            lines.add(GaugeLine(startX, startY, stopX, stopY))
        }

        // Scale lines don't have text here, so add text for min and max
        addText(startAngle, context.getString(R.string.min))
        addText(endAngle, context.getString(R.string.max))
    }

    private fun addText(angle: Float, text: String) {
        val textBounds = Rect()
        val textOffset = -32.0f
        val textOffsetX = -224f
        val angleRadians = Math.toRadians(angle.toDouble())
        val cosA = cos(angleRadians)
        val sinA = sin(angleRadians)
        scaleTextPaint.getTextBounds(text, 0, text.length, textBounds)
        val textHeight = abs(textBounds.top + textBounds.bottom) / 2f
        val x = (centerX + (radius + textOffset + textOffsetX) * cosA).toFloat()
        val y = (centerY + (radius + textOffset) * sinA).toFloat() + (textHeight / 2.0f)

        val gaugeText = GaugeText(context.getColor(android.R.color.white), SMALL_TEXT_HEIGHT, false)
        gaugeText.text = text
        gaugeText.updatePosition(x, y)

        texts.add(gaugeText)
    }

    fun setActive(active: Boolean) {
        this.active = active
        adjustActive()
    }

    private fun toggleActive(): Boolean {
        active = !active
        adjustActive()
        return active
    }

    private fun adjustActive() {
        bitmap = if (active) {
            if (activeBitmap != null) { activeBitmap } else { normalBitmap }
        } else {
            normalBitmap
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateDimensionValues(w, h)
        calculateScaleValues()
    }

    override fun onDraw(canvas: Canvas) {
        //canvas.drawRect(bounds, paintScaleLine)
        canvas.drawArc(boundsValueArc, startAngle, totalAngle, false, paintUnderlayArc)
        canvas.drawArc(boundsValueArc, startAngle, valueAngle, false, paintValueArc)
        canvas.drawArc(boundsValueArc, startAngle + valueAngle, targetAngle - valueAngle, false, paintTargetArc)

        for (line in lines) {
            canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paintScaleLine)
        }

        canvas.drawCircle(centerX, centerY, coloredCircleRadius, paintColoredCircle)

        for (text in texts) {
            text.draw(canvas)
        }

        valueText.draw(canvas)
        if (isEnabled) {
            dacText.draw(canvas)
        }

        bitmap?.let {
            canvas.drawBitmap(it, bitmapX, bitmapY, bitmapPaint)
        }

        // Center spot
        //canvas.drawCircle(centerX, centerY, 1f, paintColoredCircle)

        if (drawSpot) {
            canvas.drawCircle(spotX, spotY, spotRadius, paintSpot)
        }
    }

    fun setCurrentValue(current: Int, active: Int) {
        val newAngle = (current.toFloat() * totalAngle) / Constants.NUMBER_OF_VOLUME_STEPS.toFloat()
        valueAngle = newAngle
        if (active == 0) {
            targetAngle = newAngle
        }
        calculateValue(valueAngle)
        invalidate()
    }

    fun setDacData(text: String) {
        dacText.text = text
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (isEnabled) {
            if (gestureDetector.onTouchEvent(event)) {
                return true
            }

            if (ignoreTouches) {
                return false
            }

            touchX = event.x
            touchY = event.y
            val d = distanceFromCenter(touchX, touchY)
            if ((d > touchDistanceMin) && (d < touchDistanceMax)) {
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
        val xd = abs(x - centerX).toDouble()
        val yd = abs(y - centerY).toDouble()
        return sqrt(xd.pow(2.0) + yd.pow(2.0)).toFloat()
    }

    private fun updateTargetAngle() {
        // Calculate the angle of user touch
        val angle = getAngleForPoint(touchX, touchY)
        if ((angle < startAngle) && (angle > (endAngle - 360f))) {
            return
        }
        targetAngle = angle - startAngle
        if (angle < startAngle) {
            targetAngle += 360f
        }
        calculateValue(targetAngle)
    }

    private fun calculateValue(angle: Float): Int {
        val value = ((angle * maximumValue) / totalAngle).toInt()
        valueText.text = "$value$unit"
        updateSpot(angle)
        return value
    }

    private fun updateSpot(angle: Float) {
        val angleRadians = Math.toRadians((startAngle + angle).toDouble()).toFloat()
        spotX = centerX + (coloredCircleRadius * cos(angleRadians))
        spotY = centerY + (coloredCircleRadius * sin(angleRadians))
        drawSpot = (spotX > 0f) && (spotY > 0f)
    }

    private fun getAngleForPoint(x: Float, y: Float): Float {
        val tx = (x - centerX).toDouble()
        val ty = (y - centerY).toDouble()
        val length = sqrt(tx * tx + ty * ty)
        val r = acos(ty / length)
        var angle = toDegrees(r).toFloat()
        if (x > centerX) {
            angle = 360f - angle
        }
        angle += 90f
        if (angle > 360f) {
            angle -= 360f
        }
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
        toggleActive()
        listener?.onGaugeViewLongPress(active, id)
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