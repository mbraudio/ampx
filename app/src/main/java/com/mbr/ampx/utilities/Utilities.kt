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

    fun printSystemData(data: ByteArray) {
        val one = 1.toByte()
        Log.e("SYSTEM", "APD: " + if (data[Constants.SYSTEM_INDEX_APD] == one) "ON" else "OFF")
        Log.e("SYSTEM", "DIRECT: " + if (data[Constants.SYSTEM_INDEX_DIRECT] == one) "ON" else "OFF")
        Log.e("SYSTEM", "LOUDNESS: " + if (data[Constants.SYSTEM_INDEX_LOUDNESS] == one) "ON" else "OFF")
        Log.e("SYSTEM", "SPEAKERS_A: " + if (data[Constants.SYSTEM_INDEX_SPEAKERS_A] == one) "ON" else "OFF")
        Log.e("SYSTEM", "SPEAKERS_B: " + if (data[Constants.SYSTEM_INDEX_SPEAKERS_B] == one) "ON" else "OFF")
        Log.e("SYSTEM", "INPUT: " + getInputString(data[Constants.SYSTEM_INDEX_INPUT].toInt()))
        Log.e("SYSTEM", "POWER: " + getPowerString(data[Constants.SYSTEM_INDEX_STATE_POWER]))
        Log.e("SYSTEM", "MUTE: " + if (data[Constants.SYSTEM_INDEX_STATE_MUTE] == one) "ON" else "OFF")
    }

    fun getInputString(input: Int): String {
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

    fun getPowerString(power: Byte): String {
        when (power) {
            Constants.POWER_STATE_OFF -> return resources.getString(R.string.standby)
            Constants.POWER_STATE_POWERING_OFF -> return resources.getString(R.string.powering_off)
            Constants.POWER_STATE_POWERING_ON -> return resources.getString(R.string.powering_on)
            Constants.POWER_STATE_ON -> return resources.getString(R.string.power_on)
        }
        return resources.getString(R.string.unknown)
    }
}