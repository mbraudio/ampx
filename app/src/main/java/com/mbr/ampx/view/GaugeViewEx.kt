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
import kotlin.math.sqrt

class GaugeViewEx : View, AnimatorUpdateListener, GestureDetector.OnGestureListener {

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
    private var strokeWidthOuterArc: Float = 0f
    // Value arc(s)
    private lateinit var boundsValueArc: RectF
    private lateinit var paintValueUnderlayArc: Paint
    private lateinit var paintValueCurrentArc: Paint
    private lateinit var paintValueTargetArc: Paint
    private var strokeWidthValueArc: Float = 0f

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
        strokeWidthOuterArc = 16f
        boundsOuterArc = RectF(0f, 0f, 0f, 0f)
        paintOuterArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintOuterArc.style = Paint.Style.STROKE
        paintOuterArc.color = context.getColor(R.color.colorOuterArc)
        paintOuterArc.strokeWidth = strokeWidthOuterArc

        // Value arc(s)
        strokeWidthValueArc = 58f
        boundsValueArc = RectF(0f, 0f, 0f, 0f)
        paintValueUnderlayArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintValueUnderlayArc.style = Paint.Style.STROKE
        paintValueUnderlayArc.color = context.getColor(R.color.colorValueUnderlayArc)
        paintValueUnderlayArc.strokeWidth = strokeWidthValueArc

        paintValueCurrentArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintValueCurrentArc.style = Paint.Style.STROKE
        paintValueCurrentArc.color = context.getColor(R.color.colorValueCurrentArc)
        paintValueCurrentArc.strokeWidth = strokeWidthValueArc

        paintValueTargetArc = Paint(Paint.ANTI_ALIAS_FLAG)
        paintValueTargetArc.style = Paint.Style.STROKE
        paintValueTargetArc.color = context.getColor(R.color.colorValueTargetArc)
        paintValueTargetArc.strokeWidth = strokeWidthValueArc
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

        val distanceFromEdgeForText = 58.0f

        // Outer Arc
        val distance = strokeWidthOuterArc / 2f
        boundsOuterArc.left = bounds.left + distance + distanceFromEdgeForText
        boundsOuterArc.top = bounds.top + distance + distanceFromEdgeForText
        boundsOuterArc.right = bounds.right - distance - distanceFromEdgeForText
        boundsOuterArc.bottom = bounds.bottom - distance - distanceFromEdgeForText

        val distanceOuterArcToValueArc = 32f

        // Value arc(s)
        var strokeHalf = strokeWidthValueArc / 2f
        boundsValueArc.left = bounds.left + strokeWidthOuterArc + distanceFromEdgeForText + strokeHalf + distanceOuterArcToValueArc
        boundsValueArc.top = bounds.top + strokeWidthOuterArc + distanceFromEdgeForText + strokeHalf + distanceOuterArcToValueArc
        boundsValueArc.right = bounds.right - strokeWidthOuterArc - distanceFromEdgeForText - strokeHalf - distanceOuterArcToValueArc
        boundsValueArc.bottom = bounds.bottom - strokeWidthOuterArc - distanceFromEdgeForText - strokeHalf - distanceOuterArcToValueArc
        val diameterScaleArc = diameter - distance - distanceFromEdgeForText - distanceOuterArcToValueArc
        touchDistanceMin = diameterScaleArc - strokeWidthValueArc * 4f
        touchDistanceMax = diameterScaleArc + strokeWidthValueArc * 3f
    }

    private fun calculateScaleValues() {

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateDimensionValues(w, h)
        calculateScaleValues()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // XXX2
        canvas.drawArc(boundsOuterArc, startAngle, totalAngle, false, paintOuterArc)
        canvas.drawArc(boundsValueArc, startAngle, totalAngle, false, paintValueUnderlayArc)
        canvas.drawArc(boundsValueArc, startAngle, valueAngle, false, paintValueCurrentArc)
        canvas.drawArc(boundsValueArc, startAngle + valueAngle, targetAngle - valueAngle, false, paintValueTargetArc)

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
        val targetValue = ((targetAngle * maximumValue) / totalAngle).toInt()
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

    companion object {
        //private val TAG = GaugeView::class.java.simpleName

        // DEFAULT VALUES
        private const val DEFAULT_NUMBER_OF_DIVISIONS = 6
        private const val DEFAULT_DIVISION_VALUE = 50
        private const val DEFAULT_DIVISION_VALUE_MULTIPLIER = 1
        private const val DEFAULT_VALUE_TEXT_HEIGHT = 78.0f
    }
}