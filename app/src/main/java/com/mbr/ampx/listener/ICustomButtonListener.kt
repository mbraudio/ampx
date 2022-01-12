package com.mbr.ampx.listener

import com.mbr.ampx.view.CustomButton

interface ICustomButtonListener {
    fun onButtonClick(button: CustomButton)
    fun onButtonLongClick(button: CustomButton)
}