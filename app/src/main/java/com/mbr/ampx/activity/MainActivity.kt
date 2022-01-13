package com.mbr.ampx.activity

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.mbr.ampx.databinding.ActivityMainBinding
import com.mbr.ampx.utilities.Utilities

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        binding.viewModel = viewModel

        Utilities.resources = resources
    }
}