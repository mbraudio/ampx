package com.mbr.ampx.listener

interface IGaugeViewListener {
    fun onGaugeViewValueSelection(value: Int, max: Int, id: Int)
    fun onGaugeViewLongPress(value: Boolean, id: Int)
}