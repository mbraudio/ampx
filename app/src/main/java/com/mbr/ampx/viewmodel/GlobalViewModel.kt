package com.mbr.ampx.viewmodel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mbr.ampx.bluetooth.BlueDevice
import com.mbr.ampx.bluetooth.IBlueDeviceListener

class GlobalViewModel : ViewModel(), IBlueDeviceListener {

    private val DEVICE_NAME = "AmpX"

    // LIVE DATA
    private val _isScanning = MutableLiveData<Boolean>()
    val isScanning : LiveData<Boolean>
        get() = _isScanning

    private val _newDeviceAdded = MutableLiveData<Boolean>()
    val newDeviceAdded : LiveData<Boolean>
        get() = _newDeviceAdded
    //fun disableNewDeviceAdded() { _newDeviceAdded.value = false }

    private val _deviceStateChanged = MutableLiveData<Boolean>()
    val deviceStateChanged : LiveData<Boolean>
        get() = _deviceStateChanged
    //fun disableDeviceStateChanged() { _deviceStateChanged.value = false }

    private val _deviceDataReceived = MutableLiveData<ByteArray>()
    val deviceDataReceived : LiveData<ByteArray>
        get() = _deviceDataReceived


    private val _showTemperature = MutableLiveData<Boolean>()
    val showTemperature : LiveData<Boolean>
        get() = _showTemperature


    // VARIABLES
    var devices = ArrayList<BlueDevice>()
    fun getDevice(index: Int): BlueDevice = devices[index]

    var active: BlueDevice? = null

    private val handler = Handler(Looper.getMainLooper())

    init {
        _isScanning.value = false
        _newDeviceAdded.value = false
        _deviceStateChanged.value = false
        _showTemperature.value = false
    }

    private var bluetoothAdapter: BluetoothAdapter? = null

    fun createBluetoothAdapter(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
        bluetoothAdapter?.let { return it.isEnabled }
        return false
    }

    private fun doesExist(device: BluetoothDevice): BlueDevice? {
        for (current in devices) {
            if (current.device?.address.equals(device.address)) {
                return current
            }
        }
        return null
    }

    fun addDevice(device: BluetoothDevice) {
        device.name?.let {
            if (it != DEVICE_NAME) {
                return
            }
        }
        var blueDevice = doesExist(device)
        if (blueDevice != null) {
            blueDevice.update(device)
        } else {
            blueDevice = BlueDevice(device, this)
            devices.add(blueDevice)
        }
        _newDeviceAdded.value = true
    }

    private fun clearDisconnectedDevices() {
        val toRemove = ArrayList<BlueDevice>(devices.size)
        for (device in devices) {
            if (device.isConnected()) {
                continue
            }
            toRemove.add(device)
        }
        for (device in toRemove) {
            devices.remove(device)
        }
        toRemove.clear()
    }

    // SCAN
    fun toggleScan(): Boolean {
        return if (_isScanning.value!!) {
            stopScan()
            false
        } else {
            startScan()
            true
        }
    }

    private fun startScan() {
        clearDisconnectedDevices()
        _isScanning.value = true
        //val filter = ScanFilter.Builder().setServiceUuid(BLUETOOTH_UUID_ADVERTISING).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE).
            setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()
        // setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
        handler.post {
            bluetoothAdapter?.bluetoothLeScanner?.startScan(null/*listOf(filter)*/, settings, scanCallback)
        }
    }

    private fun stopScan() {
        if (!_isScanning.value!!) {
            return
        }
        _isScanning.value = false
        handler.post {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        }
    }

    // Bluetooth Scan Callback
    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                addDevice(it.device)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }
/*
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
*/
    }

    // IBlueDeviceListener
    override fun onConnectionStateChange(device: BlueDevice) {
        active = if (device.isConnected()) { device } else { null }
        _deviceStateChanged.value = true
    }

    override fun onDataReceived(device: BlueDevice) {
        _deviceDataReceived.value = device.data
    }

}