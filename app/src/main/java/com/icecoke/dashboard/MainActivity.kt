package com.icecoke.dashboard

import android.animation.ObjectAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    private lateinit var dashboard: Dashboard
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dashboard = findViewById(R.id.dashboard)
        val animator = ObjectAnimator.ofInt(dashboard, "currentValue", dashboard.minValue, dashboard.maxValue)
        animator.duration = 5000
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.repeatMode = ObjectAnimator.REVERSE
        animator.start()
    }
}