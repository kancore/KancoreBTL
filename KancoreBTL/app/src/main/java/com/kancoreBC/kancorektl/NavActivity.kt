package com.kancoreBC.kancorektl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.kancoreBC.kancorektl.databinding.ActivityNavBinding


class NavActivity : AppCompatActivity(), KanJoypadView.JoypadListener {
    lateinit var binding: ActivityNavBinding
    lateinit var joypadFragment: JoystickFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_nav)
    }


}