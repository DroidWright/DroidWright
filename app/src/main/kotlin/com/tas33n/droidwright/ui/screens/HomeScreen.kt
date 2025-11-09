/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.tas33n.droidwright.data.models.LogEntry
import com.tas33n.droidwright.data.models.LogLevel
import com.tas33n.droidwright.data.models.PermissionOverview
import com.tas33n.droidwright.domain.UIAutomatorEngine

@Composable
fun HomeScreen(
    automatorEngine: UIAutomatorEngine,
    permissionOverview: PermissionOverview?,
    onRequestRuntimePermissions: (Array<String>) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRefreshPermissions: () -> Unit
) {
    val logs by automatorEngine.logs.collectAsState()
    val isRunning by automatorEngine.isRunning.collectAsState()
    val currentTask by automatorEngine.currentTask.collectAsState()
    var logsExpanded by rememberSaveable { mutableStateOf(false) }

    if (logsExpanded) {
        Box(modifier = Modifier.fillMaxSize()) {
            LogsFullscreen(
                scriptLogs = logs,
                automatorEngine = automatorEngine,
                onDismiss = { logsExpanded = false }
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { StatusCard(isRunning, currentTask) }
        
        item {
            Text(
                text = "System Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        item {
            PermissionsCard(
                permissionOverview = permissionOverview,
                onRequestRuntimePermissions = onRequestRuntimePermissions,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenOverlaySettings = onOpenOverlaySettings,
                onRefreshPermissions = onRefreshPermissions
            )
        }

        item {
            FilledTonalButton(
                onClick = { logsExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("View Console Logs")
            }
        }
    }
}

@Composable
private fun StatusCard(isRunning: Boolean, currentTask: String) {
    val statusText = if (isRunning) "RUNNING" else "IDLE"
    val statusColor = if (isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "App Icon",
                    tint = if (isRunning) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "DroidWright",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isRunning) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isRunning) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRunning) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Active Task",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isRunning) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (currentTask.isEmpty()) "None" else currentTask,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRunning) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionsCard(
    permissionOverview: PermissionOverview?,
    onRequestRuntimePermissions: (Array<String>) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRefreshPermissions: () -> Unit
) {
    val runtimePermissions = permissionOverview?.runtimePermissions.orEmpty()
    val missingRuntime = runtimePermissions.filterNot { it.isGranted }
    val allGranted = missingRuntime.isEmpty() && 
                     permissionOverview?.hasAccessibility == true && 
                     permissionOverview?.hasOverlay == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            runtimePermissions.forEach { perm ->
                PermissionRow(perm.name, perm.isGranted)
            }

            PermissionRow("Accessibility service", permissionOverview?.hasAccessibility == true)
            PermissionRow("Overlay permission", permissionOverview?.hasOverlay == true)

            if (allGranted) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "All permissions granted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                
                if (missingRuntime.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            onRequestRuntimePermissions(missingRuntime.map { it.permission }.toTypedArray())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Runtime Permissions")
                    }
                }

                if (permissionOverview?.hasAccessibility == false) {
                    OutlinedButton(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Accessibility Service")
                    }
                }

                if (permissionOverview?.hasOverlay == false) {
                    OutlinedButton(
                        onClick = onOpenOverlaySettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Overlay Permission")
                    }
                }
            }

            OutlinedButton(
                onClick = onRefreshPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Refresh Status")
            }
        }
    }
}

@Composable
fun PermissionRow(name: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            color = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            shape = RoundedCornerShape(6.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isGranted) "✓" else "✗",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsFullscreen(
    scriptLogs: List<LogEntry>,
    automatorEngine: UIAutomatorEngine,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val logcatLogs by com.tas33n.droidwright.domain.LogcatReader.logcatLogs.collectAsState()

    val minTextSize = 10f
    val maxTextSize = 18f
    val zoomStep = 1f
    var logTextSizeSp by rememberSaveable { mutableStateOf(12f) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedLevel by rememberSaveable { mutableStateOf<LogLevel?>(null) }
    
    fun adjustFont(delta: Float) {
        logTextSizeSp = (logTextSizeSp + delta).coerceIn(minTextSize, maxTextSize)
    }
    val logTextSize = logTextSizeSp.sp
    
    val allLogs = (scriptLogs + logcatLogs).sortedBy { it.timestamp }
    val filteredLogs = remember(allLogs, selectedLevel, searchQuery) {
        allLogs.filter { entry ->
            val matchesLevel = selectedLevel?.let { entry.level == it } ?: true
            val matchesQuery = searchQuery.isBlank() ||
                entry.message.contains(searchQuery, ignoreCase = true)
            matchesLevel && matchesQuery
        }
    }
    
    LaunchedEffect(Unit) {
        com.tas33n.droidwright.domain.LogcatReader.startReading()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            com.tas33n.droidwright.domain.LogcatReader.stopReading()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Console Logs") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        com.tas33n.droidwright.domain.LogcatReader.clearLogs()
                        automatorEngine.clearLogs()
                    }) {
                        Icon(Icons.Default.Delete, "Clear Logs")
                    }
                    IconButton(onClick = {
                        val allLogsText = allLogs.joinToString("\n") { "[${it.level.name}] ${it.message}" }
                        clipboardManager.setText(AnnotatedString(allLogsText))
                        Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, "Copy All")
                    }
                }
            )
        },
        containerColor = Color(0xFF1E1E1E)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Font Size Controls
            Surface(
                color = Color(0xFF2D2D2D),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Font: ${logTextSizeSp.toInt()}sp",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB0BEC5),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { adjustFont(-zoomStep) },
                        enabled = logTextSizeSp > minTextSize
                    ) {
                        Icon(
                            Icons.Default.ZoomOut,
                            contentDescription = "Decrease",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { adjustFont(zoomStep) },
                        enabled = logTextSizeSp < maxTextSize
                    ) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "Increase",
                            tint = Color.White
                        )
                    }
                }
            }
            
            // Level Filters
            val filterScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(filterScroll)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedLevel == null,
                    onClick = { selectedLevel = null },
                    label = { Text("All") }
                )
                LogLevel.values().forEach { level ->
                    FilterChip(
                        selected = selectedLevel == level,
                        onClick = { selectedLevel = if (selectedLevel == level) null else level },
                        label = { Text(level.name) }
                    )
                }
            }
            
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                singleLine = true,
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            
            // Logs List
            LogsList(
                logs = filteredLogs.asReversed(),
                isCopyable = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = logTextSize,
                    lineHeight = logTextSize * 1.25f
                ),
                enableSelection = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun LogsList(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier,
    isCopyable: Boolean = false,
    textStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 16.sp
    ),
    enableSelection: Boolean = false
) {
    val content = @Composable {
        LogsListContent(
            logs = logs,
            modifier = Modifier.fillMaxSize(),
            isCopyable = isCopyable,
            textStyle = textStyle
        )
    }

    if (enableSelection) {
        SelectionContainer(modifier = modifier) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
private fun LogsListContent(
    logs: List<LogEntry>,
    modifier: Modifier,
    isCopyable: Boolean,
    textStyle: TextStyle
) {
    if (logs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No logs yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                Text(
                    text = "Execute a script to see logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(logs) { log ->
            LogEntryRow(log, isCopyable = isCopyable, textStyle = textStyle)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogEntryRow(log: LogEntry, isCopyable: Boolean, textStyle: TextStyle) {
    val color = when (log.level) {
        LogLevel.DEBUG -> Color(0xFFB0BEC5)
        LogLevel.INFO -> Color(0xFF87CEEB)
        LogLevel.WARNING -> Color(0xFFFFD700)
        LogLevel.ERROR -> Color(0xFFFF6B6B)
        LogLevel.SUCCESS -> Color(0xFF90EE90)
    }
    val logText = "[${log.level.name}] ${log.message}"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val logModifier = if (isCopyable) {
        Modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                clipboardManager.setText(AnnotatedString(logText))
                Toast.makeText(context, "Log copied", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        Modifier
    }

    val timestampText = remember(log.timestamp) { formatLogTimestamp(log.timestamp) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF252525))
            .then(logModifier)
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(0xFF8E8E8E), fontSize = 11.sp)) {
                    append("$timestampText ")
                }
                withStyle(style = SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                    append("[${log.level.name}] ")
                }
                withStyle(style = SpanStyle(color = Color(0xFFE8E8E8))) {
                    append(log.message)
                }
            },
            style = textStyle
        )
    }
}

private val logTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

private fun formatLogTimestamp(timestamp: Long): String =
    logTimeFormatter.format(Instant.ofEpochMilli(timestamp))
