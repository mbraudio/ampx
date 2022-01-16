package com.mbr.ampx.bluetooth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.mbr.ampx.R
import com.mbr.ampx.utilities.COBS
import com.mbr.ampx.utilities.Constants
import java.util.*

interface IBlueDeviceListener {
    fun onConnectionStateChange(device: BlueDevice)
    fun onDataReceived(device: BlueDevice)
}

class BlueDevice(var device: BluetoothDevice?, var listener: IBlueDeviceListener) {
    val tag = this.javaClass.simpleName

    companion object {
        const val ACTION_DEQUE_SIZE = 32
        val BLUETOOTH_UUID_SERVICE: UUID = UUID.fromString("cbba86f5-ec03-0eb0-3b45-1ce4498b1942")
        val BLUETOOTH_UUID_RX_CHARACTERISTIC: UUID = UUID.fromString("cbba88f7-ec03-0eb0-3b45-1ce4498b1942")
        val BLUETOOTH_UUID_TX_CHARACTERISTIC: UUID = UUID.fromString("cbba87f6-ec03-0eb0-3b45-1ce4498b1942")
        val BLUETOOTH_UUID_CHARACTERISTIC_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val handler = Handler(Looper.getMainLooper())

    private var connectionState: Int = BluetoothGatt.STATE_DISCONNECTED
    private var gatt: BluetoothGatt? = null
    private var service: BluetoothGattService? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    lateinit var data: ByteArray

    private val actions: ArrayDeque<BluetoothAction> = ArrayDeque(ACTION_DEQUE_SIZE)
    private var actionActive = false

    fun update(device: BluetoothDevice) { this.device = device }

    fun clearActions() {
        actions.clear()
        actionActive = false
    }

    fun isConnected(): Boolean = connectionState == BluetoothGatt.STATE_CONNECTED

    val name: String
        get() = if (device == null) {
            "Unknown"
        } else {
            val name = device!!.name ?: "Unknown"
            name
        }

    val icon: Int
        get() {
            return if (device == null) {
                R.drawable.baseline_report_problem_white_48dp
            } else {
                if (device!!.name == null) R.drawable.baseline_report_problem_white_48dp else R.drawable.baseline_speaker_white_48dp
            }
        }

    val stateName: Int
        get() {
            when (connectionState) {
                BluetoothGatt.STATE_DISCONNECTED -> return R.string.disconnected
                BluetoothGatt.STATE_CONNECTING -> return R.string.connecting_dots
                BluetoothGatt.STATE_CONNECTED -> return R.string.connected
                BluetoothGatt.STATE_DISCONNECTING -> return R.string.disconnecting_dots
            }
            return R.string.disconnected
        }

    val stateColor: Int
        get() {
            when (connectionState) {
                BluetoothGatt.STATE_DISCONNECTED -> return android.R.color.white
                BluetoothGatt.STATE_CONNECTING -> return android.R.color.white
                BluetoothGatt.STATE_CONNECTED -> return R.color.colorGradientEnd
                BluetoothGatt.STATE_DISCONNECTING -> return android.R.color.white
            }
            return android.R.color.white
        }

    fun connectOrDisconnect(context: Context) {
        handler.post {
            if (connectionState == BluetoothGatt.STATE_DISCONNECTED) {
                connect(context)
            } else if (connectionState == BluetoothGatt.STATE_CONNECTED) {
                disconnect()
            } else {
                Log.e(tag, "Connection: BUSY")
            }
        }
    }

    private fun connect(context: Context) {
        connectionState = BluetoothGatt.STATE_CONNECTING
        notifyConnectionState()
        gatt = device?.connectGatt(context, false, gattCallback)
    }

    private fun disconnect() {
        connectionState = BluetoothGatt.STATE_DISCONNECTING
        notifyConnectionState()
        gatt?.disconnect()
    }

    private fun closeGatt() {
        service = null
        rxCharacteristic = null
        txCharacteristic = null
        gatt?.close()
    }

    private fun configureServiceAndCharacteristics(gatt: BluetoothGatt?): Boolean {
        service = gatt?.getService(BLUETOOTH_UUID_SERVICE)
        service?.let {
            rxCharacteristic = it.getCharacteristic(BLUETOOTH_UUID_RX_CHARACTERISTIC)
            txCharacteristic = it.getCharacteristic(BLUETOOTH_UUID_TX_CHARACTERISTIC)
            return rxCharacteristic != null && txCharacteristic != null
        }
        return false
    }

    private fun addAction(action: BluetoothAction) {
        actions.offer(action)
        startAction()
    }

    private fun startAction() {
        if (actionActive) {
            return
        }
        val action = actions.poll() ?: return
        actionActive = true
        gatt?.let { action.execute(it) }
    }

    private fun nextAction() {
        actionActive = false
        startAction()
    }

    private fun notifyConnectionState() {
        listener.onConnectionStateChange(this)
    }

    private fun notifyDataChanged(data: ByteArray) {
        this.data = data
        listener.onDataReceived(this)
    }

    // BLUETOOTH GATT CALLBACK
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            //super.onPhyUpdate(gatt, txPhy, rxPhy, status)
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            //super.onPhyRead(gatt, txPhy, rxPhy, status)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            handler.post {

                Log.e(tag, "Connection state: $newState")
                if (status != 0) {
                    Log.e(tag, "---> ERROR !!!")
                }

                connectionState = newState

                when (connectionState) {
                    BluetoothGatt.STATE_CONNECTED -> {
                        clearActions()
                        gatt?.discoverServices()
                    }

                    BluetoothGatt.STATE_DISCONNECTED -> {
                        clearActions()
                        closeGatt()
                    }
                }

                notifyConnectionState()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            handler.post {
                Log.e(tag, "Services discovered!")

                val ok = configureServiceAndCharacteristics(gatt)
                if (ok) {
                    Log.e(tag, "SUCCESS: Service and Characteristics found!")
                    addAction(DescriptorAction(rxCharacteristic!!))
                    addAction(DescriptorAction(txCharacteristic!!))
                    requestSystemData()
                } else {
                    Log.e(tag, "ERROR: Service and Characteristics not found!")
                    disconnect()
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {

        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            handler.post {
                nextAction()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            handler.post {
                characteristic?.let { notifyDataChanged(it.value) }
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {

        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            handler.post {
                nextAction()
            }
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {

        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {

        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.e(tag, "on Mtu Changed")
        }

        override fun onServiceChanged(gatt: BluetoothGatt) {

        }

    }

    // SEND / RECEIVE
    private fun requestSystemData() {
        val data = byteArrayOf(Commands.COMMAND_SYSTEM_DATA, Commands.COMMAND_SYSTEM_DATA)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun togglePower() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_POWER, Commands.COMMAND_TOGGLE_POWER)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun toggleMute() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_MUTE, Commands.COMMAND_TOGGLE_MUTE)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun changeInput(index: Byte) {
        val crc = (Commands.COMMAND_CHANGE_INPUT + index).toByte()
        val data = byteArrayOf(Commands.COMMAND_CHANGE_INPUT, index, crc)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun toggleDirect() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_DIRECT, Commands.COMMAND_TOGGLE_DIRECT)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun toggleSpeakerA() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_SPEAKER_A, Commands.COMMAND_TOGGLE_SPEAKER_A)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun toggleSpeakerB() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_SPEAKER_B, Commands.COMMAND_TOGGLE_SPEAKER_B)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun toggleLoudness() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_LOUDNESS, Commands.COMMAND_TOGGLE_LOUDNESS)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun togglePowerAmpDirect() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_PAMP_DIRECT, Commands.COMMAND_TOGGLE_PAMP_DIRECT)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun requestCalibration(channel: Byte, delay: Byte) {
        val crc = (Commands.COMMAND_CALIBRATION + channel + delay).toByte()
        val data = byteArrayOf(Commands.COMMAND_CALIBRATION, channel, delay, crc)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun setVolume(volume: Byte) {
        val crc = (Commands.COMMAND_UPDATE_VOLUME_VALUE + volume).toByte()
        val data = byteArrayOf(Commands.COMMAND_UPDATE_VOLUME_VALUE, volume, crc)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun sendBrightnessIndex(index: Byte) {
        val crc = (Commands.COMMAND_BRIGHTNESS_INDEX + index).toByte()
        val data = byteArrayOf(Commands.COMMAND_BRIGHTNESS_INDEX, index, crc)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun sendVolumeLedValues(red: Byte, green: Byte, blue: Byte) {
        val crc = (Commands.COMMAND_VOLUME_LED_VALUES + red + green + blue).toByte()
        val data = byteArrayOf(Commands.COMMAND_VOLUME_LED_VALUES, red, green, blue, crc)
        val buffer = COBS.Encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

}