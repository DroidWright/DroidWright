/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.domain

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.tas33n.droidwright.service.AutomationAccessibilityService

/**
 * Monitors system resources to prevent ANR and system overload
 */
object ResourceMonitor {
    private const val TAG = "ResourceMonitor"
    private const val PRESSURE_CHECK_THROTTLE_MS = 1000L
    private const val PRESSURE_WARNING_THROTTLE_MS = 5000L
    private const val MIN_AVAILABLE_PERCENT = 10f
    private const val MIN_AVAILABLE_BYTES = 200L * 1024L * 1024L // 200 MB
    @Volatile private var lastPressureCheckTs = 0L
    @Volatile private var lastPressureResult = false
    @Volatile private var lastPressureWarningTs = 0L
    
    // Minimum delays to prevent ANR (in milliseconds)
    object Delays {
        const val AFTER_SWIPE = 2000L        // 2 seconds after swipe
        const val AFTER_TAP = 1000L          // 1 second after tap
        const val AFTER_SCROLL = 1500L       // 1.5 seconds after scroll
        const val BETWEEN_ITERATIONS = 3000L // 3 seconds between loop iterations
        const val ON_ERROR = 5000L           // 5 seconds on error
        const val UI_SETTLE = 2000L          // 2 seconds for UI to settle
        const val MIN_SWIPE_INTERVAL = 3000L // Minimum 3 seconds between swipes
    }
    

    /**
     * Checks if system is under memory pressure
     */
    fun isSystemUnderPressure(context: Context): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPressureCheckTs < PRESSURE_CHECK_THROTTLE_MS) {
            return lastPressureResult
        }
        
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val underPressure = if (activityManager != null) {
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                
                val totalMem = memoryInfo.totalMem.takeIf { it > 0 } ?: memoryInfo.availMem
                val availablePercent = (memoryInfo.availMem.toFloat() / totalMem.toFloat()) * 100f
                val availableBytes = memoryInfo.availMem
                
                val underPercentThreshold = availablePercent < MIN_AVAILABLE_PERCENT
                val underAbsoluteThreshold = availableBytes < MIN_AVAILABLE_BYTES
                val isLowMemory = memoryInfo.lowMemory
                val isUnderPressure = isLowMemory || (underPercentThreshold && underAbsoluteThreshold)
                
                if (isUnderPressure && now - lastPressureWarningTs > PRESSURE_WARNING_THROTTLE_MS) {
                    val formattedPercent = String.format("%.1f", availablePercent)
                    val availableMb = availableBytes / (1024 * 1024)
                    Log.w(TAG, "System under memory pressure: $formattedPercent% available (~${availableMb}MB free)")
                    lastPressureWarningTs = now
                }
                
                isUnderPressure
            } else {
                false
            }
            
            lastPressureResult = underPressure
            lastPressureCheckTs = now
            underPressure
        } catch (e: Exception) {
            Log.e(TAG, "Error checking memory pressure: ${e.message}")
            lastPressureResult = false
            lastPressureCheckTs = now
            false
        }
    }
    
    /**
     * Gets current memory usage percentage
     */
    fun getMemoryUsagePercent(): Float {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            (usedMemory.toFloat() / runtime.maxMemory().toFloat()) * 100f
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating memory usage: ${e.message}")
            0f
        }
    }
    
    /**
     * Checks if app is responsive by verifying accessibility service connection
     */
    fun isAppResponsive(): Boolean {
        return try {
            AutomationAccessibilityService.getInstance()?.rootInActiveWindow != null
        } catch (e: Exception) {
            Log.w(TAG, "App responsiveness check failed: ${e.message}")
            false
        }
    }
}
