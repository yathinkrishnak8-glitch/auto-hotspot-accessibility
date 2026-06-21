package com.example.hotspotguard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var masterSwitch: Switch
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(64, 64, 64, 64)

        statusText = TextView(this)
        statusText.text = "Hotspot Guard is OFF"
        statusText.textSize = 20f

        masterSwitch = Switch(this)
        masterSwitch.text = "Enable Auto-Hotspot "
        masterSwitch.textSize = 18f

        layout.addView(statusText)
        layout.addView(masterSwitch)
        setContentView(layout)

        prefs = getSharedPreferences("HotspotPrefs", Context.MODE_PRIVATE)
        masterSwitch.isChecked = prefs.getBoolean("is_active", false)
        updateStatusText()

        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("is_active", isChecked).apply()
            updateStatusText()
            
            if (isChecked) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }
    }

    private fun updateStatusText() {
        if (masterSwitch.isChecked) {
            statusText.text = "Guard is ON. Waiting for 5s idle time."
        } else {
            statusText.text = "Guard is OFF."
        }
    }
}


