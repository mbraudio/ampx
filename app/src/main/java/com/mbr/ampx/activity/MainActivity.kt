package com.mbr.ampx.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mbr.ampx.R
import com.mbr.ampx.utilities.Utilities

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Utilities.resources = resources
    }
}