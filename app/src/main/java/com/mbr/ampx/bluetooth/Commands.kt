package com.mbr.ampx.bluetooth

class Commands {
    companion object {
        const val COMMAND_SYSTEM_DATA = 1
        const val COMMAND_TOGGLE_POWER = 2
        const val COMMAND_TOGGLE_MUTE = 3
        const val COMMAND_CHANGE_INPUT = 4
        const val COMMAND_TOGGLE_DIRECT = 5
        const val COMMAND_TOGGLE_SPEAKER_A = 6
        const val COMMAND_TOGGLE_SPEAKER_B = 7
        const val COMMAND_TOGGLE_LOUDNESS = 8
        const val COMMAND_TOGGLE_PAMP_DIRECT = 9
        const val COMMAND_UPDATE_VOLUME_VALUE = 10
        const val COMMAND_UPDATE_BASS_VALUE = 11
        const val COMMAND_UPDATE_TREBLE_VALUE = 12
        const val COMMAND_UPDATE_BALANCE_VALUE = 13
        const val COMMAND_BRIGHTNESS_INDEX = 14
        const val COMMAND_VOLUME_LED_VALUES = 15

        const val COMMAND_CALIBRATION = 100
        const val COMMAND_CALIBRATION_DATA_1 = 101
        const val COMMAND_CALIBRATION_DATA_2 = 102
    }
}