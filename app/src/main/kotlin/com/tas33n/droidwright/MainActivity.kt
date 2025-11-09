/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tas33n.droidwright.R
import com.tas33n.droidwright.data.models.AutomationScript
import com.tas33n.droidwright.ui.screens.AboutScreen
import com.tas33n.droidwright.ui.screens.HomeScreen
import com.tas33n.droidwright.ui.screens.ScriptEditorScreen
import com.tas33n.droidwright.ui.screens.ScriptsScreen
import com.tas33n.droidwright.ui.theme.UIAutomatorAppTheme
import com.tas33n.droidwright.service.AutomationControlService
import com.tas33n.droidwright.data.repository.ScriptRepository
import com.tas33n.droidwright.domain.GitHubRelease
import com.tas33n.droidwright.domain.GitHubReleaseChecker
import com.tas33n.droidwright.ui.components.UpdateDialog
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Don't initialize database here - it's done asynchronously in LaunchedEffect
        setContent {
            UIAutomatorAppTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val permissionOverview by viewModel.permissionOverview.collectAsState()
    val isRunning by viewModel.automatorEngine.isRunning.collectAsState()
    
    // Update checker state
    var updateRelease by rememberSaveable { mutableStateOf<GitHubRelease?>(null) }
    var isCheckingUpdate by rememberSaveable { mutableStateOf(false) }
    
    // Get current version - load asynchronously to avoid blocking
    var currentVersion by remember { mutableStateOf<String?>(null) }
    
    // Initialize app and check for updates - all async to avoid blocking UI
    LaunchedEffect(Unit) {
        // Initialize repository first (non-blocking - database init happens on IO thread)
        ScriptRepository.initialize(context)
        
        // Refresh permissions on background thread (PackageManager calls can be slow)
        withContext(Dispatchers.IO) {
            viewModel.refreshPermissions(context)
        }
        
        // Load version asynchronously
        currentVersion = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                "1.0.0-beta"
            }
        }
        
        // Delay update check to avoid blocking app startup (let UI render first)
        delay(2000)
        
        // Auto-check for updates (silent, no user interruption)
        val version = currentVersion ?: return@LaunchedEffect
        try {
            val result = withTimeoutOrNull(8000) {
                GitHubReleaseChecker.checkForUpdate(version)
            }
            
            if (result != null) {
                result.fold(
                    onSuccess = { release ->
                        if (release != null) {
                            updateRelease = release
                        }
                        // Don't show toast for auto-check
                    },
                    onFailure = { error ->
                        // Silently fail on auto-check to avoid interrupting user
                        android.util.Log.d("UpdateChecker", "Update check failed: ${error.message}")
                    }
                )
            }
        } catch (e: Exception) {
            android.util.Log.d("UpdateChecker", "Update check error: ${e.message}")
        }
    }
    val isPaused by viewModel.automatorEngine.isPaused.collectAsState()
    val scripts by viewModel.scripts.collectAsState()
    val activeScriptId by viewModel.activeScriptId.collectAsState()
    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.handleRuntimePermissionResult(context, result)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importScript(context, uri)
        }
    }
    var scriptIdToExport by rememberSaveable { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        scriptIdToExport?.let { scriptId ->
            if (uri != null) {
                viewModel.exportScriptById(context, scriptId, uri)
            }
            scriptIdToExport = null
        }
    }
    val hasRequestedRuntime = rememberSaveable { mutableStateOf(false) }
    val overlayPromptActive = rememberSaveable { mutableStateOf(false) }
    val overlayPromptTriggered = rememberSaveable { mutableStateOf(false) }
    val accessibilityPromptActive = rememberSaveable { mutableStateOf(false) }
    val accessibilityPromptTriggered = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRunning, activeScriptId, scripts) {
        if (isRunning) {
            val runningName = scripts.firstOrNull { it.id == activeScriptId }?.name
                ?: context.getString(R.string.notification_title_default)
            AutomationControlService.start(context, runningName)
        } else {
            AutomationControlService.stop(context)
        }
    }

    // Repository initialization moved to update check LaunchedEffect to avoid duplicate calls

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(permissionOverview?.runtimePermissions) {
        val missing = permissionOverview?.runtimePermissions
            ?.filterNot { it.isGranted }
            ?.map { it.permission }
            ?.toTypedArray()
        if (!hasRequestedRuntime.value && !missing.isNullOrEmpty()) {
            hasRequestedRuntime.value = true
            runtimeLauncher.launch(missing)
        }
    }

    LaunchedEffect(permissionOverview?.hasOverlay) {
        when (permissionOverview?.hasOverlay) {
            false -> if (!overlayPromptTriggered.value) {
                overlayPromptTriggered.value = true
                overlayPromptActive.value = true
            }
            true -> {
                overlayPromptActive.value = false
                overlayPromptTriggered.value = false
            }
            else -> Unit
        }
    }

    LaunchedEffect(permissionOverview?.hasAccessibility) {
        when (permissionOverview?.hasAccessibility) {
            false -> if (!accessibilityPromptTriggered.value) {
                accessibilityPromptTriggered.value = true
                accessibilityPromptActive.value = true
            }
            true -> {
                accessibilityPromptActive.value = false
                accessibilityPromptTriggered.value = false
            }
            else -> Unit
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        },
        floatingActionButton = {
            if (isRunning) {
                FloatingControls(
                    isPaused = isPaused,
                    onPause = { viewModel.pauseAutomation() },
                    onResume = { viewModel.resumeAutomation() },
                    onStop = { coroutineScope.launch { viewModel.stopAutomation() } }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    automatorEngine = viewModel.automatorEngine,
                    permissionOverview = permissionOverview,
                    onRequestRuntimePermissions = {
                        runtimeLauncher.launch(it)
                    },
                    onOpenAccessibilitySettings = {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    },
                    onOpenOverlaySettings = {
                        val intent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onRefreshPermissions = { viewModel.refreshPermissions(context) }
                )
            }
            composable("scripts") {
                ScriptsScreen(
                    scripts = scripts,
                    activeScriptId = activeScriptId,
                    automatorEngine = viewModel.automatorEngine,
                    permissionOverview = permissionOverview,
                    onExecuteScript = { script ->
                        viewModel.executeScript(context, script)
                    },
                    onPauseScript = { viewModel.pauseAutomation() },
                    onResumeScript = { viewModel.resumeAutomation() },
                    onStopScript = { coroutineScope.launch { viewModel.stopAutomation() } },
                    onImportScript = {
                        importLauncher.launch("*/*")
                    },
                    onImportScriptFromUrl = { url ->
                        viewModel.importScriptFromUrl(url)
                    },
                    onRequestRuntimePermissions = {
                        runtimeLauncher.launch(it)
                    },
                    onOpenAccessibilitySettings = {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    },
                    onOpenOverlaySettings = {
                        val intent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onOpenGuide = {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/tas33n/droidwright/blob/main/Docs.md"))
                        context.startActivity(intent)
                    },
                    onOpenEditor = { script ->
                        val targetId = script?.id ?: "new"
                        navController.navigate("scriptEditor/$targetId") {
                            launchSingleTop = true
                        }
                    },
                    onDeleteScript = { script ->
                        viewModel.deleteScript(script.id)
                    },
                    onExportScript = { script ->
                        // Sanitize script name for filename (remove invalid characters)
                        val sanitizedName = script.name
                            .replace(Regex("[<>:\"/\\|?*]"), "_")
                            .trim()
                        val filename = if (sanitizedName.endsWith(".js")) {
                            sanitizedName
                        } else {
                            "$sanitizedName.js"
                        }
                        scriptIdToExport = script.id
                        exportLauncher.launch(filename)
                    }
                )
            }
            composable("scriptEditor/{scriptId}") { backStackEntry ->
                val scriptIdArg = backStackEntry.arguments?.getString("scriptId")
                val resolvedId = scriptIdArg?.takeUnless { it == "new" }
                var draft by remember(key1 = resolvedId) { mutableStateOf<MainViewModel.ScriptDraft?>(null) }
                
                LaunchedEffect(resolvedId) {
                    draft = viewModel.draftFrom(resolvedId)
                }
                
                draft?.let { currentDraft ->
                    ScriptEditorScreen(
                        draft = currentDraft,
                        onSave = {
                            viewModel.saveScript(it)
                            navController.popBackStack()
                        },
                        onValidate = { viewModel.validateScript(it) },
                        onDiscard = { navController.popBackStack() }
                    )
                }
            }
            composable("about") {
                AboutScreen(
                    onCheckUpdate = {
                        // Manual update check - show toast messages
                        coroutineScope.launch(Dispatchers.IO) {
                            val version = currentVersion ?: "1.0.0-beta"
                            isCheckingUpdate = true
                            try {
                                val result = withTimeoutOrNull(10000) {
                                    GitHubReleaseChecker.checkForUpdate(version)
                                }
                                
                                if (result != null) {
                                    result.fold(
                                        onSuccess = { release ->
                                            if (release != null) {
                                                updateRelease = release
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "You're using the latest version!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onFailure = { error ->
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Failed to check for updates: ${error.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Update check timed out", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to check for updates", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                isCheckingUpdate = false
                            }
                        }
                    }
                )
            }
        }
    }

    // Show update dialog if update is available
    updateRelease?.let { release ->
        UpdateDialog(
            release = release,
            onDismiss = { updateRelease = null },
            onDownload = { updateRelease = null }
        )
    }
    
    if (overlayPromptActive.value) {
        PermissionDialog(
            title = "Overlay permission required",
            message = "Allow display over other apps to show floating automation controls.",
            onConfirm = {
                overlayPromptActive.value = false
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            },
            onDismiss = { overlayPromptActive.value = false }
        )
    }

    if (accessibilityPromptActive.value) {
        PermissionDialog(
            title = "Accessibility service required",
            message = "Enable the automation accessibility service so scripts can interact with the UI.",
            onConfirm = {
                accessibilityPromptActive.value = false
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            onDismiss = { accessibilityPromptActive.value = false }
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        NavigationBarItem(
            icon = { Text("🏠") },
            label = { Text("Home") },
            selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
            onClick = {
                navController.navigate("home") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        NavigationBarItem(
            icon = { Text("📝") },
            label = { Text("Scripts") },
            selected = currentDestination?.hierarchy?.any { it.route == "scripts" } == true,
            onClick = {
                navController.navigate("scripts") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        NavigationBarItem(
            icon = { Text("ℹ️") },
            label = { Text("About") },
            selected = currentDestination?.hierarchy?.any { it.route == "about" } == true,
            onClick = {
                navController.navigate("about") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
fun FloatingControls(
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        FloatingActionButton(
            onClick = { if (isPaused) onResume() else onPause() },
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (isPaused) "Resume automation" else "Pause automation"
            )
        }

        FloatingActionButton(
            onClick = onStop,
            containerColor = MaterialTheme.colorScheme.errorContainer
        ) {
            Icon(
                imageVector = Icons.Rounded.Stop,
                contentDescription = "Stop automation"
            )
        }
    }
}

@Composable
fun PermissionDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Open settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(title) },
        text = { Text(message) }
    )
}
