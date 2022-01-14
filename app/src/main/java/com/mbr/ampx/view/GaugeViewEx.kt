package com.mbr.ampx.view

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.mbr.ampx.R
import com.mbr.ampx.utilities.Constants
import kotlin.math.sqrt

class GaugeViewEx : View, AnimatorUpdateListener, GestureDetector.OnGestureListener {

    companion object {
        //private val TAG = GaugeView::class.java.simpleName

        // DEFAULT VALUES
        private const val DEFAULT_NUMBER_OF_DIVISIONS = 6
        private const val DEFAULT_DIVISION_VALUE = 50
        private const val DEFAULT_DIVISION_VALUE_MULTIPLIER = 1
        private const val DEFAULT_VALUE_TEXT_HEIGHT = 78.0f

        // ADJUSTMENTS
        private const val DISTANCE_FROM_EDGE_FOR_TEXT = 16f //78.0f
        private const val OUTER_ARC_STROKE_WIDTH = 10.0f
        private const val VALUE_ARC_STROKE_WIDTH = 56.0f
        private const val SCALE_LINE_LENGTH = 40.0f
        private const val OUTER_ARC_DISTANCE_TO_VALUE_ARC = 16.0f
        private const val VALUE_ARC_DISTANCE_TO_SCALE = 24.0f
        private const val SCALE_ARC_DISTANCE_TO_UNDERLAY_CIRCLE = 68.0f
        private const val UNDERLAY_CIRCLE_WIDTH = 62.0f
        private const val COLORED_CIRCLE_WIDTH = 10.0f
        private const val SMALL_TEXT_HEIGHT = 40.0f
    }

    // TYPED VARIABLES
    private val touchEnabled = true
    private val startAngle = 120f
    private val endAngle = 420f
    private val totalAngle = endAngle - startAngle
    private var valueAngle = 0f
    private var targetAngle = 0f
    private var touchDistanceMin = 0f
    private var touchDistanceMax = 0f
    private var numberOfDivisions = 0
    private var divisionValue = 0
    private var divisionValueMultiplier = 0
    private var value = 0
    private var maximumValue = 0
    private var diameter = 0f
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

    // Outer arc
    private lateinit var boundsOuterArc: RectF
    private lateinit var paintOuterArc: Paint
    // Value arc(s)
    private lateinit var boundsValueArc: RectF
    private lateinit var paintValueUnderlayArc: Paint
    private lateinit var paintValueCurrentArc: Paint
    private lateinit var paintValueTargetArc: Paint
    // Scale lines
    private var scaleLines = ArrayList<GaugeScaleLine>()
    private lateinit var paintScaleLine: Paint
    // Inner circles
    private lateinit var paintUnderlayCircle: Paint
    private var underlayCircleRadius: Float = 0f
    private lateinit var paintColoredCircle: Paint
    private var coloredCircleRadius: Float = 0f

    private lateinit var scaleTextPaint: Paint

    private var texts = ArrayList<GaugeText>()

    // Gestures and Touches
    private lateinit var gestureDetector: GestureDetector
    private var listener: GaugeView.Listener? = null
    fun setListener(listener: GaugeView.Listener) { this.listener = listener }

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

        // XXX1
        // Outer Arc
        boundsOuterArc = RectF(0f, 0f, 0f, 0f)
        paintOuterArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintOuterArc.style = Paint.Style.STROKE
        paintOuterArc.color = context.getColor(R.color.colorOuterArc)
        paintOuterArc.strokeWidth = OUTER_ARC_STROKE_WIDTH
        paintOuterArc.strokeCap = Paint.Cap.ROUND

        // Value arc(s)
        boundsValueArc = RectF(0f, 0f, 0f, 0f)
        paintValueUnderlayArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintValueUnderlayArc.style = Paint.Style.STROKE
        paintValueUnderlayArc.color = context.getColor(R.color.colorUnderlayArc)
        paintValueUnderlayArc.strokeWidth = VALUE_ARC_STROKE_WIDTH
        paintValueUnderlayArc.strokeCap = Paint.Cap.ROUND

        paintValueCurrentArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintValueCurrentArc.style = Paint.Style.STROKE
        //paintValueCurrentArc.color = context.getColor(R.color.colorCurrentArc)
        paintValueCurrentArc.strokeWidth = VALUE_ARC_STROKE_WIDTH
        paintValueCurrentArc.strokeCap = Paint.Cap.ROUND


        paintValueTargetArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintValueTargetArc.style = Paint.Style.STROKE
        paintValueTargetArc.color = context.getColor(R.color.colorTargetArc)
        paintValueTargetArc.strokeWidth = VALUE_ARC_STROKE_WIDTH
        paintValueTargetArc.strokeCap = Paint.Cap.ROUND

        // Scale lines
        val scaleLineStrokeWidth = 4f
        paintScaleLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintScaleLine.style = Paint.Style.STROKE
        paintScaleLine.color = context.getColor(R.color.colorGradientEnd)
        paintScaleLine.strokeWidth = scaleLineStrokeWidth

        // Inner circles
        // Underlay
        paintUnderlayCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintUnderlayCircle.style = Paint.Style.STROKE
        paintUnderlayCircle.color = context.getColor(R.color.colorUnderlayCircle)
        paintUnderlayCircle.strokeWidth = UNDERLAY_CIRCLE_WIDTH

        // Colored
        paintColoredCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintColoredCircle.style = Paint.Style.STROKE
        paintColoredCircle.color = context.getColor(R.color.colorGradientEnd)
        paintColoredCircle.strokeWidth = COLORED_CIRCLE_WIDTH

        scaleTextPaint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
        scaleTextPaint.color = context.getColor(R.color.colorText)
        scaleTextPaint.style = Paint.Style.FILL_AND_STROKE
        scaleTextPaint.textSize = 38.0f
        scaleTextPaint.textAlign = Paint.Align.LEFT
        scaleTextPaint.isElegantTextHeight = true
        scaleTextPaint.typeface = Typeface.SERIF //ResourcesCompat.getFont(context, R.font.orbitron)
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.GaugeViewEx, defStyle, 0)
        numberOfDivisions = a.getInt(R.styleable.GaugeViewEx_numberOfDivisionsEx, DEFAULT_NUMBER_OF_DIVISIONS)
        divisionValue = a.getInt(R.styleable.GaugeViewEx_divisionValueEx, DEFAULT_DIVISION_VALUE)
        divisionValueMultiplier = a.getInt(R.styleable.GaugeViewEx_divisionValueMultiplierEx, DEFAULT_DIVISION_VALUE_MULTIPLIER)
        centerValueTextHeight = a.getDimension(R.styleable.GaugeViewEx_valueTextHeightEx, DEFAULT_VALUE_TEXT_HEIGHT)
        unit = a.getString(R.styleable.GaugeViewEx_unitEx)
        if (unit == null) {
            unit = resources.getString(R.string.percentage)
        }
        valueTextBold = a.getBoolean(R.styleable.GaugeViewEx_valueTextBoldEx, false)
        maximumValue = numberOfDivisions * divisionValue * divisionValueMultiplier
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
        bounds.left = paddingLeft.toFloat()
        bounds.top = paddingTop.toFloat()
        bounds.bottom = h.toFloat() - paddingEnd
        bounds.right = w.toFloat() - paddingBottom
        centerX = (bounds.left + bounds.right) / 2f
        centerY = (bounds.top + bounds.bottom) / 2f
        diameter = Math.min(bounds.right - bounds.left, bounds.bottom - bounds.top) / 2f

        // Outer Arc
        val distance = OUTER_ARC_STROKE_WIDTH / 2f
        boundsOuterArc.left = bounds.left + distance + DISTANCE_FROM_EDGE_FOR_TEXT
        boundsOuterArc.top = bounds.top + distance + DISTANCE_FROM_EDGE_FOR_TEXT
        boundsOuterArc.right = bounds.right - distance - DISTANCE_FROM_EDGE_FOR_TEXT
        boundsOuterArc.bottom = bounds.bottom - distance - DISTANCE_FROM_EDGE_FOR_TEXT

        val positions = floatArrayOf(0.0f, 1.0f)
        val colors = intArrayOf(ContextCompat.getColor(context, R.color.colorGradientStart), ContextCompat.getColor(context, R.color.colorGradientEnd))
        val gradient = SweepGradient(centerX, centerY, colors, positions)
        val gradientMatrix = Matrix()
        gradientMatrix.preRotate(startAngle - (VALUE_ARC_STROKE_WIDTH / 2f), centerX, centerY)
        gradient.setLocalMatrix(gradientMatrix)
        paintValueCurrentArc.shader = gradient

        // Value arc(s)
        val strokeHalf = VALUE_ARC_STROKE_WIDTH / 2f
        boundsValueArc.left = bounds.left + OUTER_ARC_STROKE_WIDTH + DISTANCE_FROM_EDGE_FOR_TEXT + strokeHalf + OUTER_ARC_DISTANCE_TO_VALUE_ARC
        boundsValueArc.top = bounds.top + OUTER_ARC_STROKE_WIDTH + DISTANCE_FROM_EDGE_FOR_TEXT + strokeHalf + OUTER_ARC_DISTANCE_TO_VALUE_ARC
        boundsValueArc.right = bounds.right - OUTER_ARC_STROKE_WIDTH - DISTANCE_FROM_EDGE_FOR_TEXT - strokeHalf - OUTER_ARC_DISTANCE_TO_VALUE_ARC
        boundsValueArc.bottom = bounds.bottom - OUTER_ARC_STROKE_WIDTH - DISTANCE_FROM_EDGE_FOR_TEXT - strokeHalf - OUTER_ARC_DISTANCE_TO_VALUE_ARC
        val diameterScaleArc = diameter - DISTANCE_FROM_EDGE_FOR_TEXT - OUTER_ARC_DISTANCE_TO_VALUE_ARC - OUTER_ARC_STROKE_WIDTH
        touchDistanceMin = diameterScaleArc - VALUE_ARC_STROKE_WIDTH * 4f
        touchDistanceMax = diameterScaleArc + VALUE_ARC_STROKE_WIDTH * 3f
        scaleDiameter = diameterScaleArc - VALUE_ARC_STROKE_WIDTH - VALUE_ARC_DISTANCE_TO_SCALE

        // Inner circles
        underlayCircleRadius = scaleDiameter - SCALE_LINE_LENGTH - SCALE_ARC_DISTANCE_TO_UNDERLAY_CIRCLE
        coloredCircleRadius = underlayCircleRadius - (UNDERLAY_CIRCLE_WIDTH / 2f)
    }

    private fun calculateScaleValues() {
        scaleLines.clear()
        val numberOfLines = numberOfDivisions + 1
        scaleLines = ArrayList(numberOfLines)
        val totalAngle = endAngle - startAngle
        val divisionAngle = totalAngle / numberOfDivisions
        val lineStart = scaleDiameter
        val lineEnd = scaleDiameter - SCALE_LINE_LENGTH
        val dia = diameter + 5.0f
        val textBounds = Rect()
        var info: GaugeScaleLine
        val textOffset = -28.0f
        for (i in 0 until numberOfLines) {
            val currentAngle = (startAngle + i * divisionAngle).toDouble()
            val angleRadians = Math.toRadians(currentAngle)
            val cosA = Math.cos(angleRadians)
            val sinA = Math.sin(angleRadians)
            val startX = (centerX + lineStart * cosA).toFloat()
            val startY = (centerY + lineStart * sinA).toFloat()
            val stopX = (centerX + lineEnd * cosA).toFloat()
            val stopY = (centerY + lineEnd * sinA).toFloat()
            info = GaugeScaleLine(startX, startY, stopX, stopY)
            info.text = "${i * divisionValue}"

            val textWidth = scaleTextPaint.measureText(info.text) / 2f
            scaleTextPaint.getTextBounds(info.text, 0, info.text.length, textBounds)
            val textHeight = Math.abs(textBounds.top + textBounds.bottom) / 2f
            info.textX = (centerX + (dia + textOffset) * cosA).toFloat() - textWidth
            info.textY = (centerY + (dia + textOffset) * sinA).toFloat() + textHeight

            scaleLines.add(info)
        }

        // Scale lines don't have text here, so add text for min and max
        addText(startAngle, context.getString(R.string.min))
        addText(endAngle, context.getString(R.string.max))
    }

    private fun addText(angle: Float, text: String) {
        val textBounds = Rect()
        val textOffset = -32.0f
        val textXoffset = -224f
        val angleRadians = Math.toRadians(angle.toDouble())
        val cosA = Math.cos(angleRadians)
        val sinA = Math.sin(angleRadians)
        scaleTextPaint.getTextBounds(text, 0, text.length, textBounds)
        val textHeight = Math.abs(textBounds.top + textBounds.bottom) / 2f
        val x = (centerX + (diameter + textOffset + textXoffset) * cosA).toFloat()
        val y = (centerY + (diameter + textOffset) * sinA).toFloat() + (textHeight / 2.0f)

        val gaugeText = GaugeText(context.getColor(android.R.color.white), SMALL_TEXT_HEIGHT, false)
        gaugeText.setText(text)
        gaugeText.updatePosition(x, y)

        texts.add(gaugeText)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateDimensionValues(w, h)
        calculateScaleValues()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawArc(boundsOuterArc, startAngle, totalAngle, false, paintOuterArc)
        canvas.drawArc(boundsValueArc, startAngle, totalAngle, false, paintValueUnderlayArc)
        canvas.drawArc(boundsValueArc, startAngle, valueAngle, false, paintValueCurrentArc)
        canvas.drawArc(boundsValueArc, startAngle + valueAngle, targetAngle - valueAngle, false, paintValueTargetArc)

        for (i in scaleLines.indices) {
            val info = scaleLines[i]
            if (info.drawText) {
                canvas.drawText(info.text, info.textX, info.textY, scaleTextPaint)
            }
            canvas.drawLine(info.startX, info.startY, info.endX, info.endY, paintScaleLine)
        }

        canvas.drawCircle(centerX, centerY, underlayCircleRadius, paintUnderlayCircle)
        canvas.drawCircle(centerX, centerY, coloredCircleRadius, paintColoredCircle)

        for (text in texts) {
            text.draw(canvas)
        }
    }

    private fun calculateValue(angle: Float) {
        value = (angle * maximumValue / totalAngle).toInt()
        //valueText!!.setText("" + value + unit)
    }

    fun setCurrentValue(current: Int, active: Int) {
        val percentage = current.toFloat() / Constants.NUMBER_OF_STEPS
        val newAngle = totalAngle * percentage
        valueAngle = newAngle
        if (active == 0) {
            targetAngle = newAngle
        }
        calculateValue(valueAngle)
        invalidate()
    }

    //fun setValueTextVisibility(visible: Boolean) { valueText?.visible = visible }

    override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
        valueAngle = valueAnimator.animatedValue as Float
        calculateValue(valueAngle)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (touchEnabled) {
            if (gestureDetector.onTouchEvent(event)) {
                return true
            }
            touchX = event.x
            touchY = event.y
            val d = distanceFromCenter(touchX, touchY)
            if (d > touchDistanceMin && d < touchDistanceMax) {
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        updateTargetAngle()
                        listener?.onGaugeViewValueUpdate(value.toFloat(), maximumValue.toFloat())
                    }
                    MotionEvent.ACTION_UP -> {
                        calculateValue(targetAngle)
                        listener?.onGaugeViewValueSelection(value.toFloat(), maximumValue.toFloat())
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
        val xd = Math.abs(x - centerX)
        val yd = Math.abs(y - centerY)
        return Math.sqrt(Math.pow(xd.toDouble(), 2.0) + Math.pow(yd.toDouble(), 2.0)).toFloat()
    }

    private fun updateTargetAngle() {
        // Calculate the angle of user touch
        val angle = getAngleForPoint(touchX, touchY)
        if (angle < startAngle && angle > endAngle - 360f) {
            return
        }
        targetAngle = angle - startAngle
        if (angle < startAngle) {
            targetAngle += 360f
        }
        //val targetValue = ((targetAngle * maximumValue) / totalAngle).toInt()
        //valueText!!.setText("" + targetValue + unit)
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
        if (angle > 360f) {
            angle -= 360f
        }
        return angle
    }

    // Gesture Detector Listener
    override fun onDown(e: MotionEvent): Boolean {
        return false
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
        return false
    }

    // Listener
    interface Listener {
        fun onGaugeViewValueUpdate(value: Float, max: Float)
        fun onGaugeViewValueSelection(value: Float, max: Float)
    }
}