package com.example.hotspotguard

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class HotspotAccessibilityService : AccessibilityService() {

    private val idleHandler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences
    private var wakeLock: PowerManager.WakeLock? = null
    private var isExecutingMacro = false

    private val idleRunnable = Runnable {
        val isActive = prefs.getBoolean("is_active", false)
        if (isActive && !isExecutingMacro) {
            checkAndTurnOnHotspot()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences("HotspotPrefs", Context.MODE_PRIVATE)
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HotspotGuard::CpuLock")
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes max for safety*/)
        
        resetIdleTimer()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isExecutingMacro) {
            resetIdleTimer()
        }
    }

    private fun resetIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, 5000)
    }

    private fun checkAndTurnOnHotspot() {
        isExecutingMacro = true
        
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName("com.android.settings", "com.android.settings.TetherSettings")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        
        try {
            startActivity(intent)
            
            Handler(Looper.getMainLooper()).postDelayed({
                performClickAndReturn()
            }, 600) 
            
        } catch (e: Exception) {
            e.printStackTrace()
            isExecutingMacro = false
            resetIdleTimer()
        }
    }

    private fun performClickAndReturn() {
        val rootNode = rootInActiveWindow ?: return

        // Note: Change "Wi-Fi hotspot" to "Mobile Hotspot" if using a Samsung device
        val hotspotNodes = rootNode.findAccessibilityNodeInfosByText("Wi-Fi hotspot")
        
        if (hotspotNodes.isNotEmpty()) {
            val toggleNode = hotspotNodes[0]
            
            if (!toggleNode.isChecked) {
                val clicked = toggleNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!clicked && toggleNode.parent != null) {
                    toggleNode.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        }

        performGlobalAction(GLOBAL_ACTION_BACK)
        isExecutingMacro = false
        resetIdleTimer()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.takeIf { it.isHeld }?.release()
        idleHandler.removeCallbacks(idleRunnable)
    }
}
