package com.mbr.ampx.utilities

import android.content.res.Resources
import android.util.Log
import com.mbr.ampx.R
import com.mbr.ampx.viewmodel.DacInput

object Utilities {

    lateinit var resources: Resources

    fun isDigital(index: Int): Boolean {
        return index == 0 || index == 2
    }

    fun calculateCrc(data: ByteArray): Boolean {
        val size = data.size - 2
        var crc: Byte = 0
        for (i in 0 until size) {
            crc = (data[i] + crc).toByte()
        }
        return crc == data[size]
    }

    fun byteArrayToIntArray(array: ByteArray): IntArray {
        val ints = IntArray(array.size)
        for (i in array.indices) {
            ints[i] = array[i].toUByte().toInt()
        }
        return ints
    }

    fun printSystemData(data: IntArray) {
        Log.e("SYSTEM", "APD: " + if (data[Constants.SYSTEM_INDEX_APD] == 1) "ON" else "OFF")
        Log.e("SYSTEM", "DIRECT: " + if (data[Constants.SYSTEM_INDEX_DIRECT] == 1) "ON" else "OFF")
        Log.e("SYSTEM", "LOUDNESS: " + if (data[Constants.SYSTEM_INDEX_LOUDNESS] == 1) "ON" else "OFF")
        Log.e("SYSTEM", "SPEAKERS_A: " + if (data[Constants.SYSTEM_INDEX_SPEAKERS_A] == 1) "ON" else "OFF")
        Log.e("SYSTEM", "SPEAKERS_B: " + if (data[Constants.SYSTEM_INDEX_SPEAKERS_B] == 1) "ON" else "OFF")
        Log.e("SYSTEM", "INPUT: " + getInputString(data[Constants.SYSTEM_INDEX_INPUT]))
        Log.e("SYSTEM", "POWER: " + getPowerString(data[Constants.SYSTEM_INDEX_STATE_POWER]))
        Log.e("SYSTEM", "MUTE: " + if (data[Constants.SYSTEM_INDEX_STATE_MUTE] == 1) "ON" else "OFF")
        Log.e("SYSTEM", "VOLUME KNOB LED: " + if (data[Constants.SYSTEM_INDEX_VOLUME_KNOB_LED] == 1) "ON" else "OFF")
        Log.e("SYSTEM", "DAC INPUT: ${getDacInputString(data[Constants.SYSTEM_INDEX_DAC_INPUT])} ")
        Log.e("SYSTEM", "DAC DATA: ${getDacData(data[Constants.SYSTEM_INDEX_DAC_SAMPLE_RATE], data[Constants.SYSTEM_INDEX_DAC_FORMAT])} ")
    }

    private fun getInputString(input: Int): String {
        when (input) {
            0 -> return resources.getString(R.string.cd)
            1 -> return resources.getString(R.string.network)
            2 -> return resources.getString(R.string.phono)
            3 -> return resources.getString(R.string.tuner)
            4 -> return resources.getString(R.string.aux)
            5 -> return resources.getString(R.string.recorder)
        }
        return resources.getString(R.string.unknown)
    }

    private fun getPowerString(power: Int): String {
        when (power) {
            Constants.POWER_STATE_OFF -> return resources.getString(R.string.standby)
            Constants.POWER_STATE_POWERING_OFF -> return resources.getString(R.string.powering_off)
            Constants.POWER_STATE_POWERING_ON -> return resources.getString(R.string.powering_on)
            Constants.POWER_STATE_ON -> return resources.getString(R.string.power_on)
        }
        return resources.getString(R.string.unknown)
    }

    fun getDacData(sampleRate: Int, format: Int): String {
        var text: String
        when (sampleRate) {
            Constants.PCM9211_FREQUENCY_OUT_OF_RANGE -> text = resources.getString(R.string.out_of_range)
            Constants.PCM9211_FREQUENCY_8kHz -> text = resources.getString(R.string.kHz8)
            Constants.PCM9211_FREQUENCY_11kHz -> text = resources.getString(R.string.kHz11)
            Constants.PCM9211_FREQUENCY_12kHz -> text = resources.getString(R.string.kHz12)
            Constants.PCM9211_FREQUENCY_16kHz -> text = resources.getString(R.string.kHz16)
            Constants.PCM9211_FREQUENCY_22kHz -> text = resources.getString(R.string.kHz22)
            Constants.PCM9211_FREQUENCY_24kHz -> text = resources.getString(R.string.kHz24)
            Constants.PCM9211_FREQUENCY_32kHz -> text = resources.getString(R.string.kHz32)
            Constants.PCM9211_FREQUENCY_44kHz -> text = resources.getString(R.string.kHz44)
            Constants.PCM9211_FREQUENCY_48kHz -> text = resources.getString(R.string.kHz48)
            Constants.PCM9211_FREQUENCY_64kHz -> text = resources.getString(R.string.kHz64)
            Constants.PCM9211_FREQUENCY_88kHz -> text = resources.getString(R.string.kHz88)
            Constants.PCM9211_FREQUENCY_96kHz -> text = resources.getString(R.string.kHz96)
            Constants.PCM9211_FREQUENCY_128kHz -> text = resources.getString(R.string.kHz128)
            Constants.PCM9211_FREQUENCY_176kHz -> text = resources.getString(R.string.kHz176)
            Constants.PCM9211_FREQUENCY_192kHz -> text = resources.getString(R.string.kHz196)
            else -> text = resources.getString(R.string.out_of_range)
        }

        text = when (format) {
            Constants.DAC_FORMAT_24B_I2S, Constants.DAC_FORMAT_24B_LJ, Constants.DAC_FORMAT_24B_RJ -> "$text @ 24bit"
            Constants.DAC_FORMAT_16B_RJ -> "$text @ 16bit"
            else  -> "$text @ ??bit"
        }

        return text
    }

    fun getDacInputString(input: Int): String {
        when (input) {
            Constants.PCM9211_INPUT_RXIN_2 -> return resources.getString(R.string.RXIN2)
            Constants.PCM9211_INPUT_RXIN_4 -> return resources.getString(R.string.RXIN4)
        }
        return resources.getString(R.string.unknown)
    }

/*
    fun setWindowFlag(bits: Int, on: Boolean) {
        val winParams = window.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        window.attributes = winParams
    }
*/
}