package com.mbr.ampx.utilities

import com.mbr.ampx.listener.ICustomButtonListener
import com.mbr.ampx.view.CustomButton

class ButtonGroup(count: Int) {

    private val buttons = ArrayList<CustomButton>(count)

    fun addButton(button: CustomButton) { buttons.add(button) }

    fun select(button: CustomButton): Int {
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

    fun setGroupListener(listener: ICustomButtonListener) {
        for (current in buttons) {
            current.listener = listener
        }
    }

    fun onDestroy() {
        buttons.clear()
    }
}