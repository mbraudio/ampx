package com.mbr.ampx.bluetooth

import android.bluetooth.BluetoothGatt

abstract class BluetoothAction {
    abstract fun execute(gatt: BluetoothGatt)
}