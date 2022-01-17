package com.mbr.ampx.utilities

class Constants {

    companion object {
        const val MODE_WRITE = 0
        const val MODE_READ = 1

        const val NUMBER_OF_VOLUME_STEPS = 150

        // SYSTEM INDEXES
        const val SYSTEM_INDEX_APD = 1
        const val SYSTEM_INDEX_DIRECT = 2
        const val SYSTEM_INDEX_LOUDNESS = 3
        const val SYSTEM_INDEX_SPEAKERS_A = 4
        const val SYSTEM_INDEX_SPEAKERS_B = 5
        const val SYSTEM_INDEX_INPUT = 6
        const val SYSTEM_INDEX_BRIGHTNESS_INDEX = 7
        const val SYSTEM_INDEX_VOLUME_RED = 8
        const val SYSTEM_INDEX_VOLUME_GREEN = 9
        const val SYSTEM_INDEX_VOLUME_BLUE = 10
        const val SYSTEM_INDEX_STATE_POWER = 11
        const val SYSTEM_INDEX_STATE_MUTE = 12

        // POWER
        // STATES
        const val POWER_STATE_OFF = 0
        const val POWER_STATE_POWERING_OFF = 1
        const val POWER_STATE_POWERING_ON = 2
        const val POWER_STATE_ON = 3
    }
}