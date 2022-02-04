package com.mbr.ampx.viewmodel

import com.mbr.ampx.utilities.Constants

class DacInput(val input: Int, var sampleRate: Int)

class DAC {
    private val inputs = arrayListOf<DacInput>()

    init {
        inputs.add(DacInput(Constants.PCM9211_INPUT_RXIN_2, 0))
        inputs.add(DacInput(Constants.PCM9211_INPUT_RXIN_4, 0))
    }

    fun setSampleRate(input: Int, sampleRate: Int) {
        for (di in inputs) {
            if (di.input != input) {
                continue
            }
            di.sampleRate = sampleRate
            break
        }
    }

    fun getSampleRate(input: Int) : Int {
        for (di in inputs) {
            if (di.input != input) {
                continue
            }
            return di.sampleRate
        }
        return 0
    }
}