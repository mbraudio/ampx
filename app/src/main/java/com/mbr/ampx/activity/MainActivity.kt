package com.mbr.ampx.activity

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.mbr.ampx.databinding.ActivityMainBinding
import com.mbr.ampx.dialog.ScanDialogFragment
import com.mbr.ampx.utilities.ButtonGroup
import com.mbr.ampx.utilities.Utilities
import com.mbr.ampx.view.IModernButtonListener
import com.mbr.ampx.view.ModernButton
import com.mbr.ampx.viewmodel.GlobalViewModel

class MainActivity : AppCompatActivity(), IModernButtonListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var inputGroup: ButtonGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val viewModel = ViewModelProvider(this).get(GlobalViewModel::class.java)
        binding.viewModel = viewModel

        Utilities.resources = resources

        binding.gaugeViewVolume.setCurrentValue(100, 1)

        // Buttons
        binding.buttonConnection.listener = object : IModernButtonListener {
            override fun onButtonClick(button: ModernButton) {
                ScanDialogFragment().show(supportFragmentManager, "scan")
            }

            override fun onButtonLongClick(button: ModernButton) {
                //binding.viewModel.directConnectDisconnect(applicationContext)
            }
        }

        binding.buttonPower.listener = this
        //binding.buttonMute.listener = this

        binding.buttonSettings.listener = object : IModernButtonListener {
            override fun onButtonClick(button: ModernButton) {
                //SettingsDialogFragment().show(supportFragmentManager, "scan")
            }

            override fun onButtonLongClick(button: ModernButton) {

            }
        }

        inputGroup = ButtonGroup(5)
        inputGroup.addButton(binding.inputButtonCd)
        inputGroup.addButton(binding.inputButtonNetwork)
        inputGroup.addButton(binding.inputButtonTuner)
        inputGroup.addButton(binding.inputButtonAux)
        inputGroup.addButton(binding.inputButtonRecorder)
        inputGroup.setGroupListener(this)

        // TODO: ADD REST OF BUTTONS !!!

        // Bluetooth Adapter
        createBluetoothAdapter()
    }

    /*
    fun setWindowFlag(bits: Int, on: Boolean) {
        val winParams = window.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        window.attributes = winParams
    }
    */

    private fun createBluetoothAdapter() {
        if (!binding.viewModel!!.createBluetoothAdapter(this)) {
            resultLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let {
            if (it.action == BluetoothAdapter.ACTION_REQUEST_ENABLE) {
                if (result.resultCode == Activity.RESULT_OK) {
                    Log.e("MAIN ACTIVITY", "BLUETOOTH ENABLED !!!")
                } else {
                    Log.e("MAIN ACTIVITY", "BLUETOOTH NOT ENABLED !!!")
                }
            }
        }
    }

    // IModernButtonListener
    override fun onButtonClick(button: ModernButton) {

    }

    override fun onButtonLongClick(button: ModernButton) {

    }
}