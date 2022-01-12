package com.mbr.ampx.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.mbr.ampx.utilities.Constants

class CharacteristicAction(private val mode: Int, private val characteristic: BluetoothGattCharacteristic, private val data: ByteArray) : BluetoothAction() {

    override fun execute(gatt: BluetoothGatt) {
        when (mode) {
            Constants.MODE_WRITE -> {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                characteristic.value = data
                gatt.writeCharacteristic(characteristic)
            }
            Constants.MODE_READ -> {
                gatt.readCharacteristic(characteristic)
            }
        }
    }

}