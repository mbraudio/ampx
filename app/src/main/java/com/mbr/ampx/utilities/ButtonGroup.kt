package com.mbr.ampx.utilities

import android.view.View
import com.mbr.ampx.view.ModernButton

class ButtonGroup(count: Int) {

    private val buttons = ArrayList<ModernButton>(count)

    fun addButton(button: ModernButton) { buttons.add(button) }

    fun select(button: ModernButton): Int {
        for (current in buttons) {
            current.setActive(false)
        }
        button.setActive(true)
        return buttons.indexOf(button)
    }

    fun select(index: Int) {
        for (current in buttons) {
            current.setActive(false)
        }
        if (index < 0) {
            return
        }
        val button = buttons.get(index)
        button.setActive(true)
    }

    fun setGroupListener(listener: View.OnClickListener) {
        for (current in buttons) {
            current.setOnClickListener(listener)
        }
    }

    fun onDestroy() {
        buttons.clear()
    }
}