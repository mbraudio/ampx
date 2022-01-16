package com.mbr.ampx.adapter

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mbr.ampx.R
import com.mbr.ampx.bluetooth.BlueDevice

class DeviceAdapter(private val resources: Resources) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textViewName: TextView = view.findViewById(R.id.textViewScanDeviceName)
        val textViewState: TextView = view.findViewById(R.id.textViewScanDeviceState)
    }

    private var devices: ArrayList<BlueDevice>? = ArrayList()
    fun setDevices(devices: ArrayList<BlueDevice>?) { this.devices = devices }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        devices?.let {
            val device: BlueDevice = it[position]
            holder.textViewName.text = device.name
            holder.textViewName.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(device.icon), null, null, null)
            holder.textViewState.text = resources.getString(device.stateName)
            holder.textViewState.setTextColor(resources.getColor(device.stateColor))
        }
    }

    override fun getItemCount(): Int = devices?.size ?: 0

    override fun getItemViewType(position: Int): Int = R.layout.view_recycler_device


}