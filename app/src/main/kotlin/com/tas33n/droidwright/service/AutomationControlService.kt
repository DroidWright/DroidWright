/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tas33n.droidwright.R
import com.tas33n.droidwright.domain.UIAutomatorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AutomationControlService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val notificationManager by lazy {
        ContextCompat.getSystemService(this, NotificationManager::class.java)!!
    }

    private val windowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var overlayView: View? = null
    private var overlayScriptNameText: TextView? = null
    private var overlayStatusText: TextView? = null
    private var overlayToggleButton: ImageButton? = null
    private var overlayStopButton: ImageButton? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var currentScriptName: String = ""
    
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialX = 0
    private var initialY = 0
    private var isDragging = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        observeEngineState()
    }

    override fun onDestroy() {
        removeOverlay()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentScriptName = intent.getStringExtra(EXTRA_SCRIPT_NAME) ?: getString(R.string.notification_title_default)
                val notification = buildNotification(isPaused = UIAutomatorEngine.isPaused.value, isRunning = true, task = UIAutomatorEngine.currentTask.value)
                startForeground(NOTIFICATION_ID, notification)
                showOverlay()
                updateOverlayState(UIAutomatorEngine.isPaused.value, UIAutomatorEngine.currentTask.value)
            }
            ACTION_PAUSE -> UIAutomatorEngine.pauseExecution()
            ACTION_RESUME -> UIAutomatorEngine.resumeExecution()
            ACTION_STOP -> {
                UIAutomatorEngine.stopExecution()
                stopSelf()
            }
            ACTION_UPDATE -> {
                currentScriptName = intent.getStringExtra(EXTRA_SCRIPT_NAME) ?: currentScriptName
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(UIAutomatorEngine.isPaused.value, UIAutomatorEngine.isRunning.value, UIAutomatorEngine.currentTask.value)
                )
                overlayScriptNameText?.text = currentScriptName
            }
        }
        return START_STICKY
    }

    private fun observeEngineState() {
        scope.launch {
            UIAutomatorEngine.isRunning.collectLatest { running ->
                if (running) {
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        buildNotification(UIAutomatorEngine.isPaused.value, true, UIAutomatorEngine.currentTask.value)
                    )
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }

        scope.launch {
            UIAutomatorEngine.isPaused.collectLatest { paused ->
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(paused, UIAutomatorEngine.isRunning.value, UIAutomatorEngine.currentTask.value)
                )
                updateOverlayState(paused, UIAutomatorEngine.currentTask.value)
            }
        }

        scope.launch {
            UIAutomatorEngine.currentTask.collectLatest { task ->
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(UIAutomatorEngine.isPaused.value, UIAutomatorEngine.isRunning.value, task)
                )
                updateOverlayState(UIAutomatorEngine.isPaused.value, task)
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_automation_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_automation_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPaused: Boolean, isRunning: Boolean, task: String?): Notification {
        val titleRes = if (isPaused) R.string.notification_title_paused else R.string.notification_title_running
        val content = if (!task.isNullOrEmpty()) {
            getString(R.string.notification_content_task, task)
        } else {
            getString(R.string.notification_content_default)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(getString(titleRes, currentScriptName))
            .setContentText(content)
            .setOngoing(isRunning)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val pauseResumeAction = if (isPaused) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                getString(R.string.notification_action_resume),
                getPendingIntent(ACTION_RESUME)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_action_pause),
                getPendingIntent(ACTION_PAUSE)
            ).build()
        }

        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.notification_action_stop),
            getPendingIntent(ACTION_STOP)
        ).build()

        builder.addAction(pauseResumeAction)
        builder.addAction(stopAction)

        return builder.build()
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, AutomationControlService::class.java).apply {
            this.action = action
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getService(this, action.hashCode(), intent, flags)
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        if (overlayView != null) return

        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.overlay_controls, null)
        overlayView = view
        overlayScriptNameText = view.findViewById(R.id.overlayScriptName)
        overlayStatusText = view.findViewById(R.id.overlayStatusText)
        overlayToggleButton = view.findViewById(R.id.overlayToggleButton)
        overlayStopButton = view.findViewById(R.id.overlayStopButton)

        overlayScriptNameText?.text = currentScriptName

        // Set up button click listeners
        overlayToggleButton?.setOnClickListener {
            if (UIAutomatorEngine.isPaused.value) {
                UIAutomatorEngine.resumeExecution()
            } else {
                UIAutomatorEngine.pauseExecution()
            }
        }

        overlayStopButton?.setOnClickListener {
            UIAutomatorEngine.stopExecution()
        }

        // Set up drag functionality on the container
        val container = view.findViewById<View>(R.id.overlayContainer)
        container?.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // Store initial touch position
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    overlayLayoutParams?.let {
                        initialX = it.x
                        initialY = it.y
                    }
                    isDragging = false
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    overlayLayoutParams?.let { params ->
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        // Start dragging if moved more than threshold (to avoid accidental drags)
                        if (!isDragging && (kotlin.math.abs(deltaX) > 15 || kotlin.math.abs(deltaY) > 15)) {
                            isDragging = true
                            // Cancel any pending clicks
                            v.cancelPendingInputEvents()
                        }
                        
                        if (isDragging) {
                            params.x = (initialX + deltaX).toInt()
                            params.y = (initialY + deltaY).toInt()
                            // Update gravity to TOP|START for free positioning
                            params.gravity = Gravity.TOP or Gravity.START
                            try {
                                windowManager.updateViewLayout(v, params)
                            } catch (e: Exception) {
                                android.util.Log.e("AutomationControlService", "Failed to update overlay position", e)
                            }
                        }
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    true
                }
                else -> false
            }
        }
        
        // Make buttons consume touch events to prevent drag when clicking buttons
        overlayToggleButton?.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    false // Let the button handle the click
                }
                else -> false
            }
        }
        
        overlayStopButton?.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    false // Let the button handle the click
                }
                else -> false
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 120
        }

        overlayLayoutParams = layoutParams
        windowManager.addView(view, layoutParams)
    }

    private fun updateOverlayState(isPaused: Boolean, task: String?) {
        overlayToggleButton?.setImageResource(
            if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        )
        overlayStatusText?.text = when {
            task.isNullOrBlank() -> getString(if (isPaused) R.string.overlay_status_paused else R.string.overlay_status_running)
            else -> task
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
        }
        overlayView = null
        overlayScriptNameText = null
        overlayStatusText = null
        overlayToggleButton = null
        overlayStopButton = null
        overlayLayoutParams = null
        isDragging = false
    }

    companion object {
        private const val CHANNEL_ID = "automation_controls_channel"
        private const val NOTIFICATION_ID = 7331

        const val ACTION_START = "com.tas33n.droidwright.service.AutomationControlService.START"
        const val ACTION_PAUSE = "com.tas33n.droidwright.service.AutomationControlService.PAUSE"
        const val ACTION_RESUME = "com.tas33n.droidwright.service.AutomationControlService.RESUME"
        const val ACTION_STOP = "com.tas33n.droidwright.service.AutomationControlService.STOP"
        const val ACTION_UPDATE = "com.tas33n.droidwright.service.AutomationControlService.UPDATE"

        const val EXTRA_SCRIPT_NAME = "extra_script_name"

        fun start(context: Context, scriptName: String) {
            val intent = Intent(context, AutomationControlService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SCRIPT_NAME, scriptName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun update(context: Context, scriptName: String) {
            val intent = Intent(context, AutomationControlService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_SCRIPT_NAME, scriptName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutomationControlService::class.java))
        }
    }
}
