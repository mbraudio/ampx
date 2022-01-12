package com.mbr.ampx.view

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mbr.ampx.R
import com.mbr.ampx.listener.ICustomButtonListener

class CustomButton : LinearLayout, View.OnClickListener, View.OnLongClickListener {

    private lateinit var backgroundView: ImageView
    private lateinit var activeView: ImageView
    private lateinit var textView: TextView
    private var imageAlwaysVisible = false
    private var active = false

    private var imageActiveResourceId = 0

    var listener: ICustomButtonListener? = null


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

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_custom_button, this, true)

        backgroundView = findViewById(R.id.imageViewButtonBackground)
        activeView = findViewById(R.id.imageViewButtonActive)
        setOnClickListener(this)
        setOnLongClickListener(this)
        textView = findViewById(R.id.customButtonTextView)

        active = false

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.CustomButton, 0, 0)

            val text = a.getString(R.styleable.CustomButton_buttonText)
            if (text != null) {
                textView.text = text
            }

            val topPadding = a.getDimension(R.styleable.CustomButton_buttonTextViewTopPadding, 0F)
            if (topPadding != 0F) {
                val params = textView.layoutParams as MarginLayoutParams
                params.setMargins(0, topPadding.toInt(), 0, 0)
                textView.layoutParams = params
            }

            val imageBackground = a.getResourceId(R.styleable.CustomButton_buttonImageBackground, -1)
            if (imageBackground != -1) {
                backgroundView.setBackgroundResource(imageBackground)
            }

            imageActiveResourceId = a.getResourceId(R.styleable.CustomButton_buttonImageActive, -1)
            activeView.visibility = GONE
            if (imageActiveResourceId != -1) {
                activeView.setImageResource(imageActiveResourceId)
            }

            imageAlwaysVisible = a.getBoolean(R.styleable.CustomButton_buttonImageAlwaysVisible, false)
            if (imageAlwaysVisible) {
                activeView.visibility = VISIBLE
            }

            val tintActive = a.getBoolean(R.styleable.CustomButton_buttonTintActiveImage, true)
            if (tintActive) {
                activeView.setColorFilter(resources.getColor(R.color.main), PorterDuff.Mode.MULTIPLY)
            }

            a.recycle()
        }
    }

    fun setText(text: String) { textView.text = text }

    fun setActive(active: Boolean) {
        this.active = active

        if (imageAlwaysVisible) {
            activeView.visibility = VISIBLE
            return
        }

        if (active) {
            activeView.visibility = VISIBLE
        } else {
            activeView.visibility = GONE
        }
    }

    fun toggleActive(): Boolean {
        setActive(!active)
        return active
    }

    override fun onClick(view: View?) {
        listener?.onButtonClick(this)
    }

    override fun onLongClick(view: View?): Boolean {
        listener?.onButtonLongClick(this)
        return true
    }
}