package com.mbr.ampx.activity

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.ViewModelProvider
import com.mbr.ampx.R
import com.mbr.ampx.bluetooth.Commands
import com.mbr.ampx.databinding.ActivityMainBinding
import com.mbr.ampx.dialog.ScanDialogFragment
import com.mbr.ampx.dialog.SettingsDialogFragment
import com.mbr.ampx.utilities.ButtonGroup
import com.mbr.ampx.utilities.COBS
import com.mbr.ampx.utilities.Constants
import com.mbr.ampx.utilities.Utilities
import com.mbr.ampx.utilities.Utilities.printSystemData
import com.mbr.ampx.view.GaugeViewEx
import com.mbr.ampx.view.ModernButton
import com.mbr.ampx.view.ModernSeekBar
import com.mbr.ampx.viewmodel.GlobalViewModel
import java.lang.Exception

class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener, GaugeViewEx.IListener, ModernSeekBar.IListener {

    private val tag = this.javaClass.simpleName

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

        binding.gaugeViewVolume.setListener(this)
        //binding.seekBarBalance.setListener(this)
        //binding.gaugeViewVolume.setCurrentValue(100, 1)


        // BUTTONS
        binding.buttonConnection.setOnClickListener {
            ScanDialogFragment().show(supportFragmentManager, "scan")
        }

        binding.buttonConnection.setOnLongClickListener {
            //binding.viewModel.directConnectDisconnect(applicationContext)
            false
        }

        binding.buttonPower.setOnClickListener(this)
        //binding.buttonMute.listener = this

        binding.buttonSettings.setOnClickListener {
            SettingsDialogFragment().show(supportFragmentManager, "settings")
            //it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        }

        // INPUT BUTTONS
        inputGroup = ButtonGroup(5)
        inputGroup.addButton(binding.inputButtonCd)
        inputGroup.addButton(binding.inputButtonNetwork)
        inputGroup.addButton(binding.inputButtonTuner)
        inputGroup.addButton(binding.inputButtonAux)
        inputGroup.addButton(binding.inputButtonRecorder)
        inputGroup.setGroupListener(this)

        // TONE BUTTONS
        binding.buttonDirect.setOnClickListener(this)
        binding.buttonBassBoost.setOnClickListener(this)
        binding.buttonSpeakersA.setOnClickListener(this)
        binding.buttonSpeakersB.setOnClickListener(this)

        // Bluetooth Adapter
        createBluetoothAdapter()

        // Observers
        addObservers()

        // Set transition listener
        val transitionListener = object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {

            }

            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {

            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == R.id.end || currentId == R.id.start) {
                    binding.buttonTone.toggleActive()
                }
            }

            override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {

            }
        }
        binding.motionLayout.setTransitionListener(transitionListener)
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

    private fun processData(buffer: ByteArray) {
        val data: IntArray = try {
            val temp = COBS.decode(buffer)
            Utilities.byteArrayToIntArray(temp)
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
        val enabled = data0 == 1

        when (command) {
            Commands.COMMAND_SYSTEM_DATA -> {
                setSystemData(data)
            }

            Commands.COMMAND_TOGGLE_POWER -> {
                binding.buttonPower.setActive(enabled)
            }

            Commands.COMMAND_TOGGLE_MUTE -> {
                binding.gaugeViewVolume.setActive(enabled)
            }

            Commands.COMMAND_TOGGLE_DIRECT -> {
                binding.buttonDirect.setActive(enabled)
            }

            Commands.COMMAND_CHANGE_INPUT -> {
                inputGroup.select(data0)
            }

            Commands.COMMAND_TOGGLE_SPEAKER_A -> {
                binding.buttonSpeakersA.setActive(enabled)
            }

            Commands.COMMAND_TOGGLE_SPEAKER_B -> {
                binding.buttonSpeakersB.setActive(enabled)
            }

            Commands.COMMAND_TOGGLE_LOUDNESS -> {
                binding.buttonBassBoost.setActive(enabled)
            }

            Commands.COMMAND_TOGGLE_PAMP_DIRECT -> {
                //buttonPowerAmpDirect.setActive(enabled)
            }

            Commands.COMMAND_UPDATE_VOLUME_VALUE -> {
                binding.gaugeViewVolume.setCurrentValue(data0, data[2])
                //Log.e(tag, "VOLUME: $data0")
            }

            Commands.COMMAND_UPDATE_BASS_VALUE -> {
                //Log.e(tag, "BASS: $data0")
            }

            Commands.COMMAND_UPDATE_TREBLE_VALUE -> {
                //Log.e(tag, "TREBLE: $data0")
            }

            Commands.COMMAND_UPDATE_BALANCE_VALUE -> {
                Log.e(tag, "BALANCE: $data0")
                //binding.seekBarBalance.setCurrentValue(data0, data[2])
            }

            Commands.COMMAND_CALIBRATION_DATA_1 -> {
                if (data0 == 0) {
                    text = "const uint8_t calibration[256] = { "
                }
                var i = 2
                while (i < data.size - 1) {
                    text += data[i].toString() + ", "
                    i++
                }
            }

            Commands.COMMAND_CALIBRATION_DATA_2 -> {
                if (data.size > 1) {
                    var i = 2
                    while (i < data.size) {
                        text += data[i].toString() + ", "
                        i++
                    }
                }
                text += " };"
                Log.e(tag, "Result: $text")
            }
        }
    }

    private fun setSystemData(data: IntArray?) {
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

    private fun select(data: IntArray) {
        // Brightness index
        binding.viewModel?.active?.brightnessIndex = data[Constants.SYSTEM_INDEX_BRIGHTNESS_INDEX]

        // States
        binding.buttonPower.setActive(true)
        var enabled = data[Constants.SYSTEM_INDEX_STATE_MUTE] == 1
        binding.gaugeViewVolume.setActive(enabled)

        // System
        enabled = data[Constants.SYSTEM_INDEX_DIRECT] == 1
        binding.buttonDirect.setActive(enabled)
        enabled = data[Constants.SYSTEM_INDEX_LOUDNESS] == 1
        binding.buttonBassBoost.setActive(enabled)
        enabled = data[Constants.SYSTEM_INDEX_SPEAKERS_A] == 1
        binding.buttonSpeakersA.setActive(enabled)
        enabled = data[Constants.SYSTEM_INDEX_SPEAKERS_B] == 1
        binding.buttonSpeakersB.setActive(enabled)
        val index = data[Constants.SYSTEM_INDEX_INPUT]
        inputGroup.select(index)
        //binding.gaugeViewVolume.setValueTextVisibility(true)
        binding.buttonSettings.setActive(true)
    }

    private fun deselect() {
        binding.buttonPower.setActive(false)
        binding.gaugeViewVolume.setCurrentValue(0, 0)
        binding.gaugeViewVolume.setActive(false)
        //gaugeViewVolume.setValueTextVisibility(false)
        binding.buttonDirect.setActive(false)
        binding.buttonBassBoost.setActive(false)
        binding.buttonSpeakersA.setActive(false)
        binding.buttonSpeakersB.setActive(false)
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

    // GAUGE VIEW LISTENER
    override fun onGaugeViewValueUpdate(value: Int, max: Int) {

    }

    override fun onGaugeViewValueSelection(value: Int, max: Int) {
        val index = (value * Constants.NUMBER_OF_VOLUME_STEPS) / max
        Log.e(tag, "Index: $index")
        binding.viewModel!!.active?.setVolume(index.toByte())
    }

    override fun onGaugeViewLongPress(value: Boolean) {
        //Log.e(tag, "Mute: $value")
        binding.viewModel!!.active?.setMute(value)
        // TODO: Need to add this functionality to STM32 project!!!
    }

    // Modern SeekBar IListener
    override fun onValueSelection(value: Int, seekBar: ModernSeekBar) {
        //Log.e(tag, "SEEK BAR - on Value Selection")
        binding.viewModel!!.active?.setBalance(value.toByte())
    }

    override fun onLongPress(value: Int, seekBar: ModernSeekBar) {
        //Log.e(tag, "SEEK BAR - on Long Press")
        binding.viewModel!!.active?.setBalance(value.toByte())
    }

    // VIEW ON CLICK LISTENER and ON LONG CLICK LISTENER
    override fun onClick(view: View?) {
        if (view == null) {
            return
        }

        val active = binding.viewModel!!.active ?: return

        val button = view as ModernButton

        when (view.id) {
            R.id.buttonPower -> {
                //button.toggleActive()
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

            R.id.buttonDirect -> {
                active.toggleDirect()
            }

            R.id.buttonBassBoost -> {
                active.toggleLoudness()
            }

            R.id.buttonSpeakersA -> {
                active.toggleSpeakersA()
            }

            R.id.buttonSpeakersB -> {
                active.toggleSpeakersB()
            }
        }
    }

    override fun onLongClick(view: View?): Boolean {
        // TODO: Finish this function...
        /*if (view.id == R.id.seekBarBalance) {

        }*/
        return true
    }
}