package com.mbr.ampx.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import com.mbr.ampx.utilities.Constants

abstract class BluetoothAction {
    abstract fun execute(gatt: BluetoothGatt)
}

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

class RunnableAction(private var handler: Handler, private var runnable: Runnable) : BluetoothAction() {
    override fun execute(gatt: BluetoothGatt) {
        handler.post(runnable)
    }
}

class MtuAction(private val mtu: Int) : BluetoothAction() {
    override fun execute(gatt: BluetoothGatt) {
        gatt.requestMtu(mtu)
    }
}

class PhyAction : BluetoothAction() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun execute(gatt: BluetoothGatt) {
        //gatt.readPhy();
        gatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M, BluetoothDevice.PHY_LE_2M, BluetoothDevice.PHY_OPTION_NO_PREFERRED)
    }
}