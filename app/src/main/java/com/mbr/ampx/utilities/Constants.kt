package com.mbr.ampx.utilities

object Constants {

    const val REQUEST_DELAY = 500L //ms

    // BLUETOOTH
    const val MODE_WRITE = 0
    const val MODE_READ = 1

    // POTENTIOMETERS
    const val NUMBER_OF_VOLUME_STEPS = 150

    // SYSTEM INDEXES
    const val SYSTEM_INDEX_APD = 1
    const val SYSTEM_INDEX_DIRECT = 2
    const val SYSTEM_INDEX_LOUDNESS = 3
    const val SYSTEM_INDEX_SPEAKERS_A = 4
    const val SYSTEM_INDEX_SPEAKERS_B = 5
    const val SYSTEM_INDEX_INPUT = 6
    const val SYSTEM_INDEX_BRIGHTNESS_INDEX = 7
    const val SYSTEM_INDEX_VOLUME_KNOB_LED = 8
    const val SYSTEM_INDEX_STATE_POWER = 9
    const val SYSTEM_INDEX_STATE_MUTE = 10
    const val SYSTEM_INDEX_DAC_INPUT = 11
    const val SYSTEM_INDEX_DAC_SAMPLE_RATE = 12
    const val SYSTEM_INDEX_DAC_FORMAT = 13
    const val SYSTEM_INDEX_DAC_FILTER = 14

    // POWER
    // STATES
    const val POWER_STATE_OFF = 0
    const val POWER_STATE_POWERING_OFF = 1
    const val POWER_STATE_POWERING_ON = 2
    const val POWER_STATE_ON = 3

    // DAC
    // SAMPLE RATES
    const val PCM9211_FREQUENCY_OUT_OF_RANGE = 0
    const val PCM9211_FREQUENCY_8kHz = 1
    const val PCM9211_FREQUENCY_11kHz = 2
    const val PCM9211_FREQUENCY_12kHz = 3
    const val PCM9211_FREQUENCY_16kHz = 4
    const val PCM9211_FREQUENCY_22kHz = 5
    const val PCM9211_FREQUENCY_24kHz = 6
    const val PCM9211_FREQUENCY_32kHz = 7
    const val PCM9211_FREQUENCY_44kHz = 8
    const val PCM9211_FREQUENCY_48kHz = 9
    const val PCM9211_FREQUENCY_64kHz = 10
    const val PCM9211_FREQUENCY_88kHz = 11
    const val PCM9211_FREQUENCY_96kHz = 12
    const val PCM9211_FREQUENCY_128kHz = 13
    const val PCM9211_FREQUENCY_176kHz = 14
    const val PCM9211_FREQUENCY_192kHz = 15
    // DIGITAL INPUTS
    const val PCM9211_INPUT_RXIN_2 = 0x02
    const val PCM9211_INPUT_RXIN_4 = 0x04
    // FORMATS
    const val DAC_FORMAT_24B_I2S = 0x00
    const val DAC_FORMAT_24B_LJ = 0x01
    const val DAC_FORMAT_24B_RJ = 0x02
    const val DAC_FORMAT_16B_RJ = 0x03

}