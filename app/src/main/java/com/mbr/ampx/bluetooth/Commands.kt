package com.mbr.ampx.bluetooth

class Commands {
    companion object {
        const val COMMAND_SYSTEM_DATA: Byte = 1
        const val COMMAND_TOGGLE_POWER: Byte = 2
        const val COMMAND_TOGGLE_MUTE: Byte = 3
        const val COMMAND_CHANGE_INPUT: Byte = 4
        const val COMMAND_TOGGLE_DIRECT: Byte = 5
        const val COMMAND_TOGGLE_SPEAKER_A: Byte = 6
        const val COMMAND_TOGGLE_SPEAKER_B: Byte = 7
        const val COMMAND_TOGGLE_LOUDNESS: Byte = 8
        const val COMMAND_TOGGLE_PAMP_DIRECT: Byte = 9
        const val COMMAND_UPDATE_VOLUME_VALUE: Byte = 10
        const val COMMAND_UPDATE_BASS_VALUE: Byte = 11
        const val COMMAND_UPDATE_TREBLE_VALUE: Byte = 12
        const val COMMAND_UPDATE_BALANCE_VALUE: Byte = 13
        const val COMMAND_BRIGHTNESS_INDEX: Byte = 14
        const val COMMAND_VOLUME_LED_VALUES: Byte = 15

        const val COMMAND_CALIBRATION: Byte = 100
        const val COMMAND_CALIBRATION_DATA_1: Byte = 101
        const val COMMAND_CALIBRATION_DATA_2: Byte = 102
    }
}