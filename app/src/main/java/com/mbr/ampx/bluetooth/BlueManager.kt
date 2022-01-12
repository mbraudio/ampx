package com.mbr.ampx.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.net.ipsec.ike.IkeSessionCallback
import com.mbr.ampx.utilities.SettingsData

object BlueManager {

    private val devices: ArrayList<BlueDevice> = ArrayList()

    val settingsData = SettingsData()

    var lastConnected: BlueDevice? = null
    private var active: BlueDevice? = null
    fun setActive(device: BlueDevice?) {
        active = device
        active?.let {
            lastConnected = it
        }
    }
    fun getActive(): BlueDevice? = active

    private lateinit var bluetoothAdapter: BluetoothAdapter

    var isScanning = false

    var scanListener: IScanListener? = null

    fun setBrightnessIndex(index: Byte) { settingsData.brightnessIndex = index }

    fun setVolumeLedsValues(red: Byte, green: Byte, blue: Byte) {
        settingsData.volumeRed = red
        settingsData.volumeGreen = green
        settingsData.volumeBlue = blue
    }

    fun clearDisconnectedDevices() {
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
        scanListener?.onScanResult()
    }

    fun createBluetoothAdapter(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
        return bluetoothAdapter.isEnabled
    }

    fun doesExist(device: BluetoothDevice): BlueDevice? {
        for (current in devices) {
            if (current.device?.address.equals(device.address)) {
                return current
            }
        }
        return null
    }

    fun addDevice(device: BluetoothDevice) {
        var blueDevice = doesExist(device)
        if (blueDevice != null) {
            blueDevice.update(device)
        } else {
            blueDevice = BlueDevice(device)
            devices.add(blueDevice)
        }
    }

    private val myself = this

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                myself.addDevice(it.device)
            }
            scanListener?.onScanResult()
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }

    }

    fun toggleScan(listener: IScanListener): Boolean {
        scanListener = listener
        if (isScanning) {
            stopScan()
            return false
        } else {
            startScan()
            return true
        }
    }

    private fun startScan() {
        isScanning = true
        clearDisconnectedDevices()
        val builder = ScanSettings.Builder()
        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        builder.setReportDelay(0)
        val scanner = bluetoothAdapter.bluetoothLeScanner
        scanner.startScan(null, builder.build(), scanCallback)
    }

    private fun stopScan() {
        if (!isScanning) {
            return
        }
        isScanning = false
        val scanner = bluetoothAdapter.bluetoothLeScanner
        scanner.stopScan(scanCallback)
    }

    fun directConnectDisconnect(context: Context) {
        lastConnected?.connectOrDisconnect(context)
    }
}