package com.mbr.ampx.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.mbr.ampx.R

class ModernButton : View {

    private var bounds = RectF()
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var diameter: Float = 0f

    private var active: Boolean = false
    private lateinit var paint: Paint

    private lateinit var textPaint: Paint
    private var text: String? = ""
    private var textNormal: String? = ""
    private var textActive: String? = ""
    private var textSize: Float = 0f
    private var textWidth: Float = 0f
    private var textWidthHalf: Float = 0f
    private var textHeight: Float = 0f
    private var textHeightHalf: Float = 0f

    private var circleToTextDistance: Float = 24f

    private lateinit var bitmapPaint: Paint
    private var bitmap: Bitmap? = null
    private var normalBitmap: Bitmap? = null
    private var activeBitmap: Bitmap? = null
    private var activeColorResource = 0


    constructor(context: Context) : super(context) {
        initialize(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initialize(context, attrs)
    }

    private fun initialize(context: Context, attrs: AttributeSet?) {
        loadAttributes(context, attrs)
/*
        testPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        testPaint.style = Paint.Style.STROKE
        testPaint.color = context.getColor(R.color.colorGradientStart)
        testPaint.strokeWidth = 4f
        testPaint.strokeCap = Paint.Cap.ROUND
*/
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.color = context.getColor(R.color.colorUnderlayArc)
        paint.strokeWidth = 4f
        paint.strokeCap = Paint.Cap.ROUND

        textPaint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
        textPaint.color = context.getColor(R.color.colorText)
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isElegantTextHeight = true
        textPaint.typeface = Typeface.DEFAULT
        //textPaint.typeface = ResourcesCompat.getFont(context, R.font.orbitron_regular)
        //textPaint.typeface = Typeface.createFromAsset(context.assets, "font/orbitron_medium.TTF")

        bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bitmapPaint.style = Paint.Style.FILL

        adjustActive()
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?) {
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.ModernButton, 0, 0)

            active = a.getBoolean(R.styleable.ModernButton_modernButtonActive, false)
            textNormal = a.getString(R.styleable.ModernButton_modernButtonNormalText)
            textActive = a.getString(R.styleable.ModernButton_modernButtonActiveText)
            textSize = a.getDimension(R.styleable.ModernButton_modernButtonTextSize, 0f)
            var image = a.getResourceId(R.styleable.ModernButton_modernButtonNormalImage, 0)
            if (image != 0) {
                normalBitmap = BitmapFactory.decodeResource(resources, image)
            }
            image = a.getResourceId(R.styleable.ModernButton_modernButtonActiveImage, 0)
            if (image != 0) {
                activeBitmap = BitmapFactory.decodeResource(resources, image)
            }

            activeColorResource = a.getColor(R.styleable.ModernButton_modernButtonActiveColor, 0)
            if (activeColorResource == 0) {
                activeColorResource = context.getColor(android.R.color.white)
            }

            a.recycle()
        }
    }

    fun setActive(active: Boolean) {
        this.active = active
        adjustActive()
    }

    fun toggleActive(): Boolean {
        active = !active
        adjustActive()
        return active
    }

    private fun adjustActive() {
        if (active) {
            bitmapPaint.colorFilter = PorterDuffColorFilter(activeColorResource, PorterDuff.Mode.MULTIPLY)
            text = textActive
            bitmap = if (activeBitmap != null) { activeBitmap } else { normalBitmap }
        } else {
            bitmapPaint.colorFilter = PorterDuffColorFilter(context.getColor(android.R.color.white), PorterDuff.Mode.MULTIPLY)
            text = textNormal
            bitmap = normalBitmap
        }
        measureText()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minimumWidth = paddingStart + paddingEnd
        val w = resolveSizeAndState(minimumWidth, widthMeasureSpec, 1)
        val minimumHeight = paddingBottom + paddingTop + textHeight.toInt() + diameter.toInt()
        val h = resolveSizeAndState(minimumHeight, heightMeasureSpec, 1)
        setMeasuredDimension(w, h)
    }

    fun measureText() {
        textWidth = 0f
        textHeight = 0f

        text?.let {
            val textBounds = Rect()
            textWidth = textPaint.measureText(it)
            textPaint.getTextBounds(text, 0, it.length, textBounds)
            textHeight = Math.abs(textBounds.top + textBounds.bottom).toFloat()
        }

        textWidthHalf = textWidth / 2f
        textHeightHalf = textHeight / 2f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        measureText()
        bounds.left = paddingStart.toFloat()
        bounds.top = paddingTop.toFloat()
        bounds.bottom = h.toFloat() - paddingBottom
        bounds.right = w.toFloat() - paddingEnd
        centerX = (bounds.left + bounds.right) / 2f
        if (text == null || text == "") {
            centerY = (bounds.top + bounds.bottom) / 2f
            diameter = Math.min(bounds.right - bounds.left, bounds.bottom - bounds.top) / 2f
        } else {
            centerY = (bounds.top + bounds.bottom - textHeight - circleToTextDistance) / 2f
            diameter = Math.min(bounds.right - bounds.left, bounds.bottom - bounds.top - (textHeight * 2f) - circleToTextDistance) / 2f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //canvas.drawRect(bounds, testPaint)
        canvas.drawCircle(centerX, centerY, diameter, paint)

        text?.let {
            canvas.drawText(it, centerX - textWidthHalf, centerY + diameter + textHeight + circleToTextDistance, textPaint)
        }

        bitmap?.let {
            canvas.drawBitmap(it, centerX - it.width / 2f, centerY - it.height / 2f, bitmapPaint)
        }

    }
}