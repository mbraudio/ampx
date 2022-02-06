package com.mbr.ampx.viewmodel

import com.mbr.ampx.utilities.Constants

class DacInput(val input: Int, var sampleRate: Int, var format: Int)

class DAC {
    private val inputs = arrayListOf<DacInput>()

    init {
        inputs.add(DacInput(Constants.PCM9211_INPUT_RXIN_2, 0, 0))
        inputs.add(DacInput(Constants.PCM9211_INPUT_RXIN_4, 0, 0))
    }

    fun setData(input: Int, sampleRate: Int, format: Int) {
        for (di in inputs) {
            if (di.input != input) {
                continue
            }
            di.sampleRate = sampleRate
            di.format = format
            break
        }
    }

    fun getData(input: Int) : DacInput? {
        for (di in inputs) {
            if (di.input != input) {
                continue
            }
            return di
        }
        return null
    }
}