package com.mbr.ampx.view

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.mbr.ampx.R
import com.mbr.ampx.utilities.Constants
import java.util.ArrayList
import kotlin.math.sqrt

class GaugeView : View, GestureDetector.OnGestureListener {

    private var bounds = RectF()
    private var centerX = 0f
    private var centerY = 0f
    private val touchEnabled = true
    private var touchX = 0f
    private var touchY = 0f
    private var touchDistanceMin = 0f
    private var touchDistanceMax = 0f
    private var paintOuterArc = Paint()
    private var boundsOuterArc = RectF()
    private var lineStrokeOuterArc = 0f
    private var distanceOuterArcToUnderlayArc = 0f
    private var boundsArcScale = RectF()
    private var diameterScaleArc = 0f
    private var paintUnderlayArc = Paint()
    private var lineStrokeUnderlayArc = 0f
    private var paintOverlayArc = Paint()
    private var lineStrokeOverlayArc = 0f
    private var paintTargetArc = Paint()
    private var lineStrokeTargetArc = 0f
    private var distanceArcToInnerCircle = 0f

    // Inner circle
    private var paintInnerCircle = Paint()
    private var lineStrokeInnerCircle = 0f
    private var diameterInnerCircle = 0f
    private var innerArc2ToInnerCircle = 0f

    // Scale Lines
    private var scaleLines = ArrayList<GaugeScaleLine>()
    private var paintScaleLine = Paint()

    // VARIABLES
    private val startAngle = 120f
    private val endAngle = 420f
    private val totalAngle = endAngle - startAngle
    private var valueAngle = 0f
    private var targetAngle = 0f
    private var numberOfDivisions = 0
    private var divisionValue = 0
    private var divisionValueMultiplier = 0
    private var value = 0
    private var maximumValue = 0
    private var diameter = 0f

    // TEXT
    private var scaleTextPaint = Paint()

    // CENTER TEXT
    private var centerValueTextHeight = 0f
    private var valueTextBold = false

    // Texts
    private var valueText: GaugeText? = null
    private var unit: String? = null

    // Gestures and Touches
    private lateinit var gestureDetector: GestureDetector
    private var listener: Listener? = null
    fun setListener(listener: Listener) { this.listener = listener }

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

        bounds = RectF(0f, 0f, 0f, 0f)

        val lineStrokeScaleLine = 8.0f
        paintScaleLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintScaleLine.style = Paint.Style.STROKE
        paintScaleLine.strokeWidth = lineStrokeScaleLine
        paintScaleLine.color = context.getColor(android.R.color.black)

        // Outer Arc
        lineStrokeOuterArc = 4f
        boundsOuterArc = RectF(0f, 0f, 0f, 0f)
        paintOuterArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintOuterArc.style = Paint.Style.STROKE
        paintOuterArc.color = context.getColor(R.color.colorScaleOuterArc)
        paintOuterArc.strokeWidth = lineStrokeOuterArc

        // Target Arc
        lineStrokeTargetArc = 44f
        paintTargetArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintTargetArc.style = Paint.Style.STROKE
        paintTargetArc.color = context.getColor(R.color.colorScaleSelectedArc)
        paintTargetArc.strokeWidth = lineStrokeTargetArc

        // Underlay Arc
        boundsArcScale = RectF(0f, 0f, 0f, 0f)
        lineStrokeUnderlayArc = 44.0f
        paintUnderlayArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintUnderlayArc.style = Paint.Style.STROKE
        paintUnderlayArc.color = context.getColor(R.color.colorScaleUnderlayArc)
        paintUnderlayArc.strokeWidth = lineStrokeUnderlayArc

        // Overlay Arc
        lineStrokeOverlayArc = lineStrokeUnderlayArc
        paintOverlayArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintOverlayArc.style = Paint.Style.STROKE
        paintOverlayArc.color = context.getColor(R.color.colorScaleOverlayArc)
        paintOverlayArc.strokeWidth = lineStrokeOverlayArc

        // Inner Circle
        lineStrokeInnerCircle = 4.0f
        paintInnerCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintInnerCircle.style = Paint.Style.STROKE
        paintInnerCircle.color = context.getColor(R.color.colorScaleInnerCircle)
        paintInnerCircle.strokeWidth = lineStrokeInnerCircle
        innerArc2ToInnerCircle = 0.0f
        val textColor = context.getColor(R.color.colorText)
        // Scale Text Paint
        scaleTextPaint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
        scaleTextPaint.color = textColor
        scaleTextPaint.style = Paint.Style.FILL_AND_STROKE
        scaleTextPaint.textSize = 38.0f
        scaleTextPaint.textAlign = Paint.Align.LEFT
        scaleTextPaint.isElegantTextHeight = true
        scaleTextPaint.typeface = Typeface.SANS_SERIF

        // Texts
        valueText = GaugeText(textColor, centerValueTextHeight, valueTextBold)
        gestureDetector = GestureDetector(context, this)
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.GaugeView, defStyle, 0)
        numberOfDivisions = a.getInt(R.styleable.GaugeView_numberOfDivisions, DEFAULT_NUMBER_OF_DIVISIONS)
        divisionValue = a.getInt(R.styleable.GaugeView_divisionValue, DEFAULT_DIVISION_VALUE)
        divisionValueMultiplier = a.getInt(R.styleable.GaugeView_divisionValueMultiplier, DEFAULT_DIVISION_VALUE_MULTIPLIER)
        centerValueTextHeight = a.getDimension(R.styleable.GaugeView_valueTextHeight, DEFAULT_VALUE_TEXT_HEIGHT)
        unit = a.getString(R.styleable.GaugeView_unit)
        if (unit == null) {
            unit = resources.getString(R.string.percentage)
        }
        valueTextBold = a.getBoolean(R.styleable.GaugeView_valueTextBold, false)
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
        bounds.left = paddingStart.toFloat()
        bounds.top = paddingTop.toFloat()
        bounds.bottom = h.toFloat() - paddingBottom
        bounds.right = w.toFloat() - paddingEnd
        centerX = (bounds.left + bounds.right) / 2f
        centerY = (bounds.top + bounds.bottom) / 2f
        diameter = Math.min(bounds.right - bounds.left, bounds.bottom - bounds.top) / 2f

        val distanceFromEdgeForText = 58.0f

        // Outer Arc
        var distance = lineStrokeOuterArc / 2f
        boundsOuterArc.left = bounds.left + distance + distanceFromEdgeForText
        boundsOuterArc.top = bounds.top + distance + distanceFromEdgeForText
        boundsOuterArc.right = bounds.right - distance - distanceFromEdgeForText
        boundsOuterArc.bottom = bounds.bottom - distance - distanceFromEdgeForText
        distanceOuterArcToUnderlayArc = 8f

        // Underlay & Overlay Arc
        distance += lineStrokeOverlayArc / 2f
        diameterScaleArc = diameter - distance - distanceFromEdgeForText - distanceOuterArcToUnderlayArc
        boundsArcScale.left = bounds.left + distance + distanceFromEdgeForText + distanceOuterArcToUnderlayArc
        boundsArcScale.top = bounds.top + distance + distanceFromEdgeForText + distanceOuterArcToUnderlayArc
        boundsArcScale.right = bounds.right - distance - distanceFromEdgeForText - distanceOuterArcToUnderlayArc
        boundsArcScale.bottom = bounds.bottom - distance - distanceFromEdgeForText - distanceOuterArcToUnderlayArc
        distanceArcToInnerCircle = 16f
        touchDistanceMin = diameterScaleArc - lineStrokeOverlayArc * 4f
        touchDistanceMax = diameterScaleArc + lineStrokeOverlayArc * 3f

        // Inner Circle
        distance = lineStrokeOverlayArc / 2f + lineStrokeInnerCircle / 2f + distanceArcToInnerCircle
        diameterInnerCircle = diameterScaleArc - distance - innerArc2ToInnerCircle
        calculateValue(0f)
        //valueText!!.updatePosition(centerX, centerY + (diameter - centerValueTextHeight) / 2f, textBounds)
    }

    private fun calculateScaleValues() {
        scaleLines.clear()
        val numberOfLines = numberOfDivisions + 1
        scaleLines = ArrayList(numberOfLines)
        val totalAngle = endAngle - startAngle
        val divisionAngle = totalAngle / numberOfDivisions
        val lineStart = diameterScaleArc - lineStrokeOverlayArc / 2f
        val lineEnd = diameterScaleArc + lineStrokeOverlayArc / 2f
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
            info.text = "" + i * divisionValue
            val textWidth = scaleTextPaint.measureText(info.text) / 2f
            scaleTextPaint.getTextBounds(info.text, 0, info.text.length, textBounds)
            val textHeight = Math.abs(textBounds.top + textBounds.bottom) / 2f
            info.textX = (centerX + (dia + textOffset) * cosA).toFloat() - textWidth
            info.textY = (centerY + (dia + textOffset) * sinA).toFloat() + textHeight
            scaleLines.add(info)
        }

        // Draw text only on 1st, final and middle positions
        scaleLines[0].drawText = true
        scaleLines[scaleLines.size - 1].drawText = true
        scaleLines[scaleLines.size / 2].drawText = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateDimensionValues(w, h)
        calculateScaleValues()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(boundsOuterArc, startAngle, totalAngle, false, paintOuterArc)
        canvas.drawArc(boundsArcScale, startAngle, totalAngle, false, paintUnderlayArc)
        canvas.drawArc(boundsArcScale, startAngle, valueAngle, false, paintOverlayArc)
        canvas.drawArc(boundsArcScale, startAngle + valueAngle, targetAngle - valueAngle, false, paintTargetArc)
        canvas.drawCircle(centerX, centerY, diameterInnerCircle, paintInnerCircle)
        for (i in scaleLines.indices) {
            val info = scaleLines[i]
            if (info.drawText) {
                canvas.drawText(info.text, info.textX, info.textY, scaleTextPaint)
            }
            canvas.drawLine(info.startX, info.startY, info.endX, info.endY, paintScaleLine)
        }
        valueText!!.draw(canvas)
    }

    private fun calculateValue(angle: Float) {
        value = (angle * maximumValue / totalAngle).toInt()
        valueText!!.text = "$value" + unit
    }

    fun setCurrentValue(current: Int, active: Int) {
        val percentage = current.toFloat() / Constants.NUMBER_OF_VOLUME_STEPS
        val newAngle = totalAngle * percentage
        valueAngle = newAngle
        if (active == 0) {
            targetAngle = newAngle
        }
        calculateValue(valueAngle)
        invalidate()
        /*
        if (valueAnimator != null) {
            valueAnimator.removeAllUpdateListeners();
            valueAnimator = null;
        }
        final long diff = ((long)Math.abs(valueAngle - newAngle) * 12) / 10;
        valueAnimator = ValueAnimator.ofFloat(valueAngle, newAngle);
        valueAnimator.setDuration(diff);
        valueAnimator.addUpdateListener(this);
        valueAnimator.start();*/
    }

    //fun setValueTextVisibility(visible: Boolean) { valueText?.visible = visible }

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
        val targetValue = ((targetAngle * maximumValue) / totalAngle).toInt()
        valueText!!.text = "$targetValue" + unit
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

    companion object {
        //private val TAG = GaugeView::class.java.simpleName

        // DEFAULT VALUES
        private const val DEFAULT_NUMBER_OF_DIVISIONS = 6
        private const val DEFAULT_DIVISION_VALUE = 50
        private const val DEFAULT_DIVISION_VALUE_MULTIPLIER = 1
        private const val DEFAULT_VALUE_TEXT_HEIGHT = 78.0f
    }
}