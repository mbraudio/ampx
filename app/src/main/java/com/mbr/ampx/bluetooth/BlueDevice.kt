package com.mbr.ampx.bluetooth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.mbr.ampx.R
import com.mbr.ampx.utilities.COBS
import com.mbr.ampx.utilities.Constants
import com.mbr.ampx.viewmodel.DAC
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

interface IBlueDeviceListener {
    fun onConnectionStateChange(device: BlueDevice)
    fun onDataReceived(device: BlueDevice)
}

class BlueDevice(var device: BluetoothDevice?, var listener: IBlueDeviceListener) {
    val tag = this.javaClass.simpleName

    companion object {
        //const val MTU_MAX = 517
        const val MTU_MAX_AVAILABLE = 247
        //const val ACTION_DEQUE_SIZE = 32
        val BLUETOOTH_UUID_SERVICE: UUID = UUID.fromString("cbba86f5-ec03-0eb0-3b45-1ce4498b1942")
        val BLUETOOTH_UUID_RX_CHARACTERISTIC: UUID = UUID.fromString("cbba88f7-ec03-0eb0-3b45-1ce4498b1942")
        val BLUETOOTH_UUID_TX_CHARACTERISTIC: UUID = UUID.fromString("cbba87f6-ec03-0eb0-3b45-1ce4498b1942")
        val BLUETOOTH_UUID_CHARACTERISTIC_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        //val BLUETOOTH_UUID_ADVERTISING = ParcelUuid.fromString("0000180a-0000-1000-8000-00805f9b34fb")
    }

    private val handler = Handler(Looper.getMainLooper())

    private var connectionState: Int = BluetoothGatt.STATE_DISCONNECTED
    private var gatt: BluetoothGatt? = null
    private var service: BluetoothGattService? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    lateinit var data: ByteArray

    private val actions = ConcurrentLinkedQueue<BluetoothAction>()
    private var actionActive = false

    private var mtu = 20

    // Settings
    var brightnessIndex: Int = 0
    var volumeLed: Int = 0

    val dac = DAC()

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
                R.drawable.round_report_problem_white_48
            } else {
                if (device!!.name == null) R.drawable.round_report_problem_white_48 else R.drawable.round_speaker_white_48
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
        gatt = null
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

    @Synchronized
    private fun addAction(action: BluetoothAction) {
        actions.offer(action)
        startAction()
    }

    @Synchronized
    private fun startAction() {
        if (actionActive) {
            return
        }
        val action = actions.poll() ?: return
        actionActive = true
        gatt?.let { action.execute(it) }
    }

    @Synchronized
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
                    addAction(MtuAction(MTU_MAX_AVAILABLE))
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
            resolveMtu(mtu)
            nextAction()
        }

        override fun onServiceChanged(gatt: BluetoothGatt) {

        }

    }

    private fun resolveMtu(mtu: Int) {
        this.mtu = mtu - 3
        Log.e(tag, "ON MTU CHANGED: $mtu -> Real value: ${this.mtu}")
    }

    // SEND / RECEIVE
    private fun requestSystemData() {
        val data = byteArrayOf(Commands.COMMAND_SYSTEM_DATA.toByte(), Commands.COMMAND_SYSTEM_DATA.toByte())
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun togglePower() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_POWER.toByte(), Commands.COMMAND_TOGGLE_POWER.toByte())
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun setMute(mute: Boolean) {
        val value = if (mute) 1 else 0
        val crc = (Commands.COMMAND_TOGGLE_MUTE + value).toByte()
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_MUTE.toByte(), value.toByte(), crc)
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun changeInput(index: Byte) {
        val crc = (Commands.COMMAND_CHANGE_INPUT + index).toByte()
        val data = byteArrayOf(Commands.COMMAND_CHANGE_INPUT.toByte(), index, crc)
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    // When using only 2 same bytes, crc calc is not needed! :)
    fun toggleDirect() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_DIRECT.toByte(), Commands.COMMAND_TOGGLE_DIRECT.toByte())
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun toggleSpeakersA() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_SPEAKER_A.toByte(), Commands.COMMAND_TOGGLE_SPEAKER_A.toByte())
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun toggleSpeakersB() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_SPEAKER_B.toByte(), Commands.COMMAND_TOGGLE_SPEAKER_B.toByte())
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun toggleLoudness() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_LOUDNESS.toByte(), Commands.COMMAND_TOGGLE_LOUDNESS.toByte())
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun togglePowerAmpDirect() {
        val data = byteArrayOf(Commands.COMMAND_TOGGLE_PAMP_DIRECT.toByte(), Commands.COMMAND_TOGGLE_PAMP_DIRECT.toByte())
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun requestCalibration(channel: Byte, delay: Byte) {
        val crc: Byte = (Commands.COMMAND_CALIBRATION.toByte() + channel + delay).toByte()
        val data = byteArrayOf(Commands.COMMAND_CALIBRATION.toByte(), channel, delay, crc)
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun setVolume(value: Byte) {
        val crc = (Commands.COMMAND_UPDATE_VOLUME_VALUE.toByte() + value).toByte()
        val data = byteArrayOf(Commands.COMMAND_UPDATE_VOLUME_VALUE.toByte(), value, crc)
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun setBass(value: Byte) {
        val crc = (Commands.COMMAND_UPDATE_BASS_VALUE.toByte() + value).toByte()
        val data = byteArrayOf(Commands.COMMAND_UPDATE_BASS_VALUE.toByte(), value, crc)
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun setTreble(value: Byte) {
        val crc = (Commands.COMMAND_UPDATE_TREBLE_VALUE.toByte() + value).toByte()
        val data = byteArrayOf(Commands.COMMAND_UPDATE_TREBLE_VALUE.toByte(), value, crc)
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun setBalance(value: Byte) {
        val crc = (Commands.COMMAND_UPDATE_BALANCE_VALUE.toByte() + value).toByte()
        val data = byteArrayOf(Commands.COMMAND_UPDATE_BALANCE_VALUE.toByte(), value, crc)
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun sendBrightnessIndex(index: Byte) {
        val crc = (Commands.COMMAND_BRIGHTNESS_INDEX.toByte() + index).toByte()
        val data = byteArrayOf(Commands.COMMAND_BRIGHTNESS_INDEX.toByte(), index, crc)
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

    fun enableVolumeKnobLed(value: Byte) {
        val crc = (Commands.COMMAND_SET_VOLUME_KNOB_LED.toByte() + value).toByte()
        val data = byteArrayOf(Commands.COMMAND_SET_VOLUME_KNOB_LED.toByte(), value, crc)
        val buffer = COBS.encode(data)
        addAction(CharacteristicAction(Constants.MODE_WRITE, txCharacteristic!!, buffer))
    }

}
