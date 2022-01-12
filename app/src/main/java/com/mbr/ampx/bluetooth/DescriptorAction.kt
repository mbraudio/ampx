package com.mbr.ampx.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

class DescriptorAction(private val characteristic: BluetoothGattCharacteristic) : BluetoothAction() {

    override fun execute(gatt: BluetoothGatt) {
        val descriptor = characteristic.getDescriptor(BlueDevice.BLUETOOTH_UUID_CHARACTERISTIC_DESCRIPTOR)
        val properties = characteristic.properties
        if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.setCharacteristicNotification(characteristic, true)
            gatt.writeDescriptor(descriptor)
        }
        if ((properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            gatt.setCharacteristicNotification(characteristic, true)
            gatt.writeDescriptor(descriptor)
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
    }
}