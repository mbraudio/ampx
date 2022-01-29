package com.mbr.ampx.utilities

import android.content.res.Resources
import android.util.Log
import com.mbr.ampx.R

object Utilities {

    lateinit var resources: Resources

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