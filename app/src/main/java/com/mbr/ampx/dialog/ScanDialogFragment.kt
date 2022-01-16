package com.mbr.ampx.dialog

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.mbr.ampx.R
import com.mbr.ampx.adapter.DeviceAdapter
import com.mbr.ampx.databinding.DialogScanBinding
import com.mbr.ampx.listener.RecyclerTouchListener
import com.mbr.ampx.viewmodel.GlobalViewModel

interface IRecyclerClickListener {
    fun onRecyclerItemClick(position: Int)
    fun onRecyclerLongClick(position: Int)
}

class ScanDialogFragment : DialogFragment(), View.OnClickListener, IRecyclerClickListener {

    private val TAG = this.javaClass.simpleName

    /*companion object {
        private const val CLOSE_DELAY = 800 // ms
    }*/

    private lateinit var binding: DialogScanBinding
    private lateinit var recyclerViewAdapter: DeviceAdapter
    //private val handler = Handler(Looper.getMainLooper())


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        val activity = requireActivity()

        binding = DialogScanBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        binding.buttonScanScan.setOnClickListener(this)
        binding.buttonScanScan.isEnabled = false

        binding.buttonScanClose.setOnClickListener(this)

        val recycler = binding.recyclerViewScan
        recycler.layoutManager = LinearLayoutManager(context)
        recyclerViewAdapter = DeviceAdapter(resources)
        recycler.adapter = recyclerViewAdapter

        val recyclerTouchListener = RecyclerTouchListener(requireContext(), recycler, this)
        recycler.addOnItemTouchListener(recyclerTouchListener)

        // View model
        val viewModel = ViewModelProvider(requireActivity()).get(GlobalViewModel::class.java)
        binding.viewModel = viewModel

        // Observers
        addObservers()

        // Dialog
        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        checkForPermissions(activity.applicationContext)

        return dialog
    }

    private fun addObservers() {
        val model = binding.viewModel!!

        model.isScanning.observe(this, {
            adjustGui(it)
        })

        model.newDeviceAdded.observe(this, {
            recyclerViewAdapter.setDevices(null)
            recyclerViewAdapter.setDevices(model.devices)
            recyclerViewAdapter.notifyDataSetChanged()
        })
    }

    private fun checkForPermissions(context: Context) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestMultiplePermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        } else {
            enableScanViews(true)
        }
    }

    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            Log.e(TAG, "${it.key} = ${it.value}")
        }
        var granted = true
        for (perm in permissions) {
            if (!perm.value) {
                granted = false
                break
            }
        }
        enableScanViews(granted)
    }

    private fun enableScanViews(enable: Boolean) {
        binding.buttonScanScan.isEnabled = enable
        /*if (binding.viewModel.active == null && !binding.viewModel.isScanning) {
            val scanning = binding.viewModel.toggleScan()
            adjustGui(scanning)
        }*/
    }

    private fun adjustGui(isScanning: Boolean) {
        binding.buttonScanScan.text = (resources.getString(if (isScanning) R.string.stop else R.string.scan))
        binding.progressBarScanning.visibility = if (isScanning) View.VISIBLE else View.INVISIBLE
    }

    // View.OnClickListener
    override fun onClick(view: View?) {
        view?.let {
            when (it.id) {
                R.id.buttonScanScan -> {
                    val scanning = binding.viewModel!!.toggleScan()
                    adjustGui(scanning)
                }
                R.id.buttonScanClose -> {
                    dismiss()
                }
            }
        }
    }

    // IRecyclerClickListener
    override fun onRecyclerItemClick(position: Int) {
        val device = binding.viewModel!!.getDevice(position)
        context?.let { device.connectOrDisconnect(it) }
    }

    override fun onRecyclerLongClick(position: Int) {

    }

    /*
    // IScanListener
    override fun onScanResult() {
        handler.post {
            recyclerViewAdapter.notifyDataSetChanged()
        }
    }
    */
}