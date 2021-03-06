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
import com.mbr.ampx.listener.IGaugeViewListener
import com.mbr.ampx.utilities.ButtonGroup
import com.mbr.ampx.utilities.COBS
import com.mbr.ampx.utilities.Constants
import com.mbr.ampx.utilities.Utilities
import com.mbr.ampx.utilities.Utilities.printSystemData
import com.mbr.ampx.view.GaugeViewSimple
import com.mbr.ampx.view.ModernButton
import com.mbr.ampx.viewmodel.DacInput
import com.mbr.ampx.viewmodel.GlobalViewModel
import java.lang.Exception

class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener, IGaugeViewListener {

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

        // Gauge Views
        binding.gaugeViewVolume.setListener(this)
        binding.gaugeViewBass.setListener(this)
        binding.gaugeViewTreble.setListener(this)
        binding.gaugeViewBalance.setListener(this)

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
            //binding.viewModel!!.active?.requestCalibration(0, 70) //0, 62
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
                if (startId == R.id.end || startId == R.id.start) {
                    binding.buttonTone.toggleActive()
                }
            }

            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {

            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {

            }

            override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {

            }
        }
        binding.motionLayout.setTransitionListener(transitionListener)
    }

    private fun addObservers() {
        val model = binding.viewModel!!

        model.deviceStateChanged.observe(this) {
            val status = model.active != null && model.active!!.isConnected()
            binding.buttonConnection.setActive(status)
            if (!status) {
                ready = false
                setSystemData(null)
            }
        }

        model.deviceDataReceived.observe(this) {
            processData(it)
        }

        model.showTemperature.observe(this) {
            setupTemperatureViews()
        }
    }

    private fun processData(buffer: ByteArray) {
        val data: IntArray = try {
            val temp = COBS.decode(buffer)
            // When using CALIBRATOR disable CRC check!!!
            if (!Utilities.calculateCrc(temp)) {
                Log.e(tag, "ERROR: Received data has invalid CRC!")
                return
            }
            Utilities.byteArrayToIntArray(temp)
        } catch (ex: Exception) {
            Log.e(tag, "ERROR: Received data has failed to decode!")
            ex.printStackTrace()
            return
        }

        if (data.size < 3) {
            return
        }

        val command = data[0]
        val data0 = data[1]
        val enabled = data0 == 1
        // Do not extract data[2] here, some commands don't have it

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
                setInputType(data0)
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

            Commands.COMMAND_UPDATE_POTENTIOMETERS -> {
                try {
                    binding.gaugeViewVolume.setCurrentValue(data[1], data[2])
                    binding.gaugeViewBass.setCurrentValue(data[3], data[4])
                    binding.gaugeViewTreble.setCurrentValue(data[5], data[6])
                    binding.gaugeViewBalance.setCurrentValue(data[7], data[8])
                } catch (ex: Exception)  { }
            }

            Commands.COMMAND_UPDATE_TEMPERATURE -> {
                //Log.e(tag, "TEMPERATURE: R:$data0 | L:${data[2]}")
                val right = "$data0??"
                val left = "${data[2]}??"
                binding.temperatureViewRight.text = right
                binding.temperatureViewLeft.text = left
            }

            Commands.COMMAND_UPDATE_DAC_DATA -> {
                // Set new DAC sample rate from device and store it, needed for input changes
                binding.viewModel?.dac?.setData(data0, data[2], data[3])
                // If digital input, update sample rate
                if (Utilities.isDigital(data[2])) {
                    setDacData(data[2], data[3])
                }
                Log.e(tag, "DAC DATA: ${Utilities.getDacData(data[2], data[3])}")
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
        binding.viewModel?.let {
            // Set DAC sample rate from device and store it, needed for input changes
            it.dac.setData(data[Constants.SYSTEM_INDEX_DAC_INPUT], data[Constants.SYSTEM_INDEX_DAC_SAMPLE_RATE], data[Constants.SYSTEM_INDEX_DAC_FORMAT])
            it.active?.let { a ->
                // Brightness index
                a.brightnessIndex = data[Constants.SYSTEM_INDEX_BRIGHTNESS_INDEX]
                // Volume led enabled
                a.volumeLed = data[Constants.SYSTEM_INDEX_VOLUME_KNOB_LED]
                // DAC Filter
                a.dacFilter = data[Constants.SYSTEM_INDEX_DAC_FILTER]
            }
        }
        
        // States
        binding.buttonPower.setActive(true)

        var enabled = data[Constants.SYSTEM_INDEX_STATE_MUTE] == 1
        binding.gaugeViewVolume.setActive(enabled)
        binding.gaugeViewVolume.isEnabled = true

        // DAC
        setDacData(data[Constants.SYSTEM_INDEX_DAC_SAMPLE_RATE], data[Constants.SYSTEM_INDEX_DAC_FORMAT])

        binding.gaugeViewBass.isEnabled = true
        binding.gaugeViewTreble.isEnabled = true
        binding.gaugeViewBalance.isEnabled = true

        // System
        enabled = data[Constants.SYSTEM_INDEX_DIRECT] == 1
        binding.buttonDirect.setActive(enabled)
        binding.buttonDirect.isEnabled = true

        enabled = data[Constants.SYSTEM_INDEX_LOUDNESS] == 1
        binding.buttonBassBoost.setActive(enabled)
        binding.buttonBassBoost.isEnabled = true

        enabled = data[Constants.SYSTEM_INDEX_SPEAKERS_A] == 1
        binding.buttonSpeakersA.setActive(enabled)
        binding.buttonSpeakersA.isEnabled = true

        enabled = data[Constants.SYSTEM_INDEX_SPEAKERS_B] == 1
        binding.buttonSpeakersB.setActive(enabled)
        binding.buttonSpeakersB.isEnabled = true

        val index = data[Constants.SYSTEM_INDEX_INPUT]
        inputGroup.select(index)
        inputGroup.setEnabled(true)
        setInputType(index)

        binding.buttonSettings.setActive(true)
        binding.buttonSettings.isEnabled = true

        // TEMPERATURE
        setupTemperatureViews()
    }

    private fun deselect() {
        binding.buttonPower.setActive(false)
        //binding.buttonPower.isEnabled = false

        binding.gaugeViewVolume.setCurrentValue(0, 0)
        binding.gaugeViewVolume.setActive(false)
        binding.gaugeViewVolume.isEnabled = false
        binding.gaugeViewVolume.setDacData("")

        binding.gaugeViewBass.setCurrentValue(GaugeViewSimple.DEFAULT_VALUE_HALF, 0)
        binding.gaugeViewBass.isEnabled = false

        binding.gaugeViewTreble.setCurrentValue(GaugeViewSimple.DEFAULT_VALUE_HALF, 0)
        binding.gaugeViewTreble.isEnabled = false

        binding.gaugeViewBalance.setCurrentValue(GaugeViewSimple.DEFAULT_VALUE_HALF, 0)
        binding.gaugeViewBalance.isEnabled = false

        binding.buttonDirect.setActive(false)
        binding.buttonDirect.isEnabled = false

        binding.buttonBassBoost.setActive(false)
        binding.buttonBassBoost.isEnabled = false

        binding.buttonSpeakersA.setActive(false)
        binding.buttonSpeakersA.isEnabled = false

        binding.buttonSpeakersB.setActive(false)
        binding.buttonSpeakersB.isEnabled = false

        inputGroup.select(-1)
        inputGroup.setEnabled(false)

        binding.buttonSettings.setActive(false)
        binding.buttonSettings.isEnabled = false

        // TEMPERATURE
        binding.temperatureViewLeft.visibility = View.INVISIBLE
        binding.temperatureViewRight.visibility = View.INVISIBLE
    }

    private fun setupTemperatureViews() {
        val show = binding.viewModel!!.showTemperature.value == true
        if (show && ready) {
            binding.temperatureViewLeft.visibility = View.VISIBLE
            binding.temperatureViewLeft.text = getString(R.string.temp_zero)
            binding.temperatureViewRight.visibility = View.VISIBLE
            binding.temperatureViewRight.text = getString(R.string.temp_zero)
        } else {
            binding.temperatureViewLeft.visibility = View.INVISIBLE
            binding.temperatureViewRight.visibility = View.INVISIBLE
        }
    }

    private fun setDacData(sampleRate: Int, format: Int) {
        binding.gaugeViewVolume.setDacData(Utilities.getDacData(sampleRate, format))
    }

    private fun setInputType(index: Int) {
        val digital = Utilities.isDigital(index)
        val input = if (index == 0) Constants.PCM9211_INPUT_RXIN_2 else Constants.PCM9211_INPUT_RXIN_4
        if (digital) {
            binding.viewModel?.let {
                val data: DacInput? = it.dac.getData(input)
                data?.let { d ->
                    binding.gaugeViewVolume.setDacData(Utilities.getDacData(d.sampleRate, d.format))
                }
            }
        } else {
            binding.gaugeViewVolume.setDacData("")
        }
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

    // VIEW ON CLICK LISTENER
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
                setInputType(index)
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

    // VIEW ON LONG CLICK LISTENER
    override fun onLongClick(view: View?): Boolean {
        return true
    }

    // GAUGE VIEW LISTENER
    override fun onGaugeViewValueSelection(value: Int, max: Int, id: Int) {
        val active = binding.viewModel!!.active ?: return

        when (id) {
            R.id.gaugeViewVolume -> {
                val index = (value * Constants.NUMBER_OF_VOLUME_STEPS) / max
                Log.e(tag, "Index: $index")
                active.setVolume(index.toByte())
            }

            R.id.gaugeViewBass -> {
                active.setBass(value.toByte())
            }

            R.id.gaugeViewTreble -> {
                active.setTreble(value.toByte())
            }

            R.id.gaugeViewBalance -> {
                active.setBalance(value.toByte())
            }
        }
    }

    override fun onGaugeViewLongPress(value: Boolean, id: Int) {
        val active = binding.viewModel!!.active ?: return

        when (id) {
            R.id.gaugeViewVolume -> {
                active.setMute(value)
            }

            R.id.gaugeViewBass -> {
                active.setBass(GaugeViewSimple.DEFAULT_VALUE_HALF.toByte())
            }

            R.id.gaugeViewTreble -> {
                active.setTreble(GaugeViewSimple.DEFAULT_VALUE_HALF.toByte())
            }

            R.id.gaugeViewBalance -> {
                active.setBalance(GaugeViewSimple.DEFAULT_VALUE_HALF.toByte())
            }
        }
    }
}