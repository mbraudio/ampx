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
import com.mbr.ampx.R
import com.mbr.ampx.bluetooth.Commands
import com.mbr.ampx.databinding.ActivityMainBinding
import com.mbr.ampx.dialog.ScanDialogFragment
import com.mbr.ampx.utilities.ButtonGroup
import com.mbr.ampx.utilities.COBS
import com.mbr.ampx.utilities.Constants
import com.mbr.ampx.utilities.Utilities
import com.mbr.ampx.utilities.Utilities.printSystemData
import com.mbr.ampx.view.IModernButtonListener
import com.mbr.ampx.view.ModernButton
import com.mbr.ampx.viewmodel.GlobalViewModel
import java.lang.Exception

class MainActivity : AppCompatActivity(), IModernButtonListener {

    private val tag = this.javaClass.simpleName

    private val zero = 0.toByte()
    private val one = 1.toByte()

    private var text: String = ""

    private lateinit var binding: ActivityMainBinding
    private lateinit var inputGroup: ButtonGroup

    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val viewModel = ViewModelProvider(this).get(GlobalViewModel::class.java)
        binding.viewModel = viewModel

        Utilities.resources = resources

        //binding.gaugeViewVolume.setCurrentValue(100, 1)

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

        // Observers
        addObservers()
    }

    private fun addObservers() {
        val model = binding.viewModel!!

        model.deviceStateChanged.observe(this, {
            val status = model.active != null && model.active!!.isConnected()
            binding.buttonConnection.setActive(status)
            if (!status) {
                ready = false
                setSystemData(null)
            }
        })

        model.deviceDataReceived.observe(this, {
            processData(it)
        })
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

    private fun processData(buffer: ByteArray) {
        val data: ByteArray = try {
            COBS.decode(buffer)
        } catch (ex: Exception) {
            Log.e(tag, "ERROR: Received data has failed to decode!")
            ex.printStackTrace()
            return
        }
        if (data.isEmpty()) {
            return
        }

        // When using CALIBRATOR disable CRC check!!!
        /*final boolean valid = Utilities.calculateCrc(data);
        if (!valid) {
            Log.e(TAG, "ERROR: Received data has invalid CRC!");
            return;
        }*/
        val command = data[0]
        val data0 = data[1]
        val enabled = data0 == one

        when (command) {
            Commands.COMMAND_SYSTEM_DATA -> {
                setSystemData(data)
            }

            Commands.COMMAND_TOGGLE_POWER -> {
                binding.buttonPower.setActive(enabled)
            }

            Commands.COMMAND_TOGGLE_MUTE -> {
                //buttonMute.setImageResource(if (enabled) R.drawable.mute_button else 0)
            }

            Commands.COMMAND_TOGGLE_DIRECT -> {
                //buttonDirect.setActive(enabled)
            }

            Commands.COMMAND_CHANGE_INPUT -> {
                inputGroup.select(data0.toInt())
            }

            Commands.COMMAND_TOGGLE_SPEAKER_A -> {
                //buttonSpeakersA.setActive(enabled)
            }

            Commands.COMMAND_TOGGLE_SPEAKER_B -> {
                //buttonSpeakersB.setActive(enabled)
            }

            Commands.COMMAND_TOGGLE_LOUDNESS -> {
                //buttonLoudness.setActive(enabled)
            }

            Commands.COMMAND_TOGGLE_PAMP_DIRECT -> {
                //buttonPowerAmpDirect.setActive(enabled)
            }

            Commands.COMMAND_UPDATE_VOLUME_VALUE -> {
                binding.gaugeViewVolume.setCurrentValue(data0.toInt(), data[2].toInt())
                Log.e(tag, "VOLUME: $data0")
            }

            Commands.COMMAND_UPDATE_BASS_VALUE -> {
                Log.e(tag, "BASS: $data0")
            }

            Commands.COMMAND_UPDATE_TREBLE_VALUE -> {
                Log.e(tag, "TREBLE: $data0")
            }

            Commands.COMMAND_UPDATE_BALANCE_VALUE -> {
                //Log.e(TAG, "# Value: " + data0);
                Log.e(tag, "BALANCE: $data0")
            }

            Commands.COMMAND_CALIBRATION_DATA_1 -> {
                if (data0 == zero) {
                    text = "const uint8_t calibration[256] = { "
                }
                var i = 2
                while (i < data.size - 1) {
                    text += data[i].toInt().toString() + ", "
                    i++
                }
            }

            Commands.COMMAND_CALIBRATION_DATA_2 -> {
                if (data.size > 1) {
                    var i = 2
                    while (i < data.size) {
                        text += data[i].toInt().toString() + ", "
                        i++
                    }
                }
                text += " };"
                Log.e(tag, "Result: $text")
            }
        }
    }

    private fun setSystemData(data: ByteArray?) {
        if (data != null) {
            ready = true
            printSystemData(data)
            // Power State
            val on = data[Constants.SYSTEM_INDEX_STATE_POWER] == Constants.POWER_STATE_ON
            binding.buttonPower.setActive(on)
            if (on) {
                select(data)
            } else {
                deselect()
            }
        } else {
            deselect()
        }
    }

    private fun select(data: ByteArray) {
        //var active = false
        //val manager: BlueManager = BlueManager.getInstance()
        //manager.setBrightnessIndex(data[Constants.SYSTEM_INDEX_BRIGHTNESS_INDEX])
        //manager.setVolumeLedsValues(data[Constants.SYSTEM_INDEX_VOLUME_RED], data[Constants.SYSTEM_INDEX_VOLUME_GREEN], data[Constants.SYSTEM_INDEX_VOLUME_BLUE])

        // States
        binding.buttonPower.setActive(true)
        //active = data[Constants.SYSTEM_INDEX_STATE_MUTE] == one
        //buttonMute.setImageResource(if (active) R.drawable.mute_button else 0)

        // System
        //active = data[Constants.SYSTEM_INDEX_DIRECT] == one
        //binding.buttonDirect.setActive(active)
        //active = data[Constants.SYSTEM_INDEX_LOUDNESS] == one
        //buttonLoudness.setActive(active)
        //active = data[Constants.SYSTEM_INDEX_SPEAKERS_A] == one
        //buttonSpeakersA.setActive(active)
        //active = data[Constants.SYSTEM_INDEX_SPEAKERS_B] == one
        //buttonSpeakersB.setActive(active)
        val index = data[Constants.SYSTEM_INDEX_INPUT].toInt()
        inputGroup.select(index)
        //binding.gaugeViewVolume.setValueTextVisibility(true)
        binding.buttonSettings.setActive(true)
    }

    private fun deselect() {
        binding.buttonPower.setActive(false)
        //buttonMute.setImageResource(0)
        binding.gaugeViewVolume.setCurrentValue(0, 0)
        //gaugeViewVolume.setValueTextVisibility(false)
        //buttonDirect.setActive(false)
        //buttonLoudness.setActive(false)
        //buttonSpeakersA.setActive(false)
        //buttonSpeakersB.setActive(false)
        inputGroup.select(-1)
        binding.buttonSettings.setActive(false)
    }

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
        val active = binding.viewModel!!.active ?: return

        when (button.id) {
            R.id.buttonPower -> {
                button.toggleActive()
                active.togglePower()
            }

            R.id.inputButtonCd,
            R.id.inputButtonNetwork,
            R.id.inputButtonTuner,
            R.id.inputButtonAux,
            R.id.inputButtonRecorder -> {
                val index = inputGroup.select(button)
                active.changeInput(index.toByte())
                Log.e(tag, "Input selected: $index")
            }
        }
    }

    override fun onButtonLongClick(button: ModernButton) {

    }
}