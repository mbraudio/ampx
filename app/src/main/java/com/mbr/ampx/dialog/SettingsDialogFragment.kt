package com.mbr.ampx.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.mbr.ampx.R
import com.mbr.ampx.databinding.DialogSettingsBinding
import com.mbr.ampx.viewmodel.GlobalViewModel

class SettingsDialogFragment : DialogFragment(), View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private val maxBrightnessIndex = 4

    private lateinit var binding: DialogSettingsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)

        binding = DialogSettingsBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        // View model
        val viewModel = ViewModelProvider(requireActivity()).get(GlobalViewModel::class.java)
        binding.viewModel = viewModel

        // Binding
        binding.buttonSettingsClose.setOnClickListener(this)

        // SeekBar
        binding.seekBarBrightness.max = maxBrightnessIndex
        viewModel.active?.let {
            binding.seekBarBrightness.progress = it.brightnessIndex
            binding.switchVolumeLed.isChecked = it.volumeLed == 1
        }

        binding.seekBarBrightness.setOnSeekBarChangeListener(this)
        binding.switchVolumeLed.setOnCheckedChangeListener { _, b ->
            val state = if (b) 1 else 0
            viewModel.active?.enableVolumeKnobLed(state.toByte())
        }

        binding.switchTemperature.isChecked = viewModel.showTemperature.value == true
        binding.switchTemperature.setOnCheckedChangeListener { _, b ->
            viewModel.setShowTemperature(b)
        }

        // Dialog
        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    // OnClickListener
    override fun onClick(view: View?) {
        view?.let {
            if (it.id == R.id.buttonSettingsClose) {
                dismiss()
            }
        }
    }

    // OnSeekBarChangeListener
    override fun onProgressChanged(seekBar: SeekBar?, i: Int, b: Boolean) {
        seekBar?.let { bar ->
            if (bar.id == R.id.seekBarBrightness) {
                binding.viewModel!!.active?.let {
                    it.brightnessIndex = i
                    it.sendBrightnessIndex(i.toByte())
                }
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }

}