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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tas33n.droidwright.data.models.AutomationScript
import com.tas33n.droidwright.data.models.PermissionOverview
import com.tas33n.droidwright.domain.UIAutomatorEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsScreen(
    scripts: List<AutomationScript>,
    activeScriptId: String?,
    automatorEngine: UIAutomatorEngine,
    permissionOverview: PermissionOverview?,
    onExecuteScript: (AutomationScript) -> Unit,
    onPauseScript: () -> Unit,
    onResumeScript: () -> Unit,
    onStopScript: () -> Unit,
    onImportScript: () -> Unit,
    onImportScriptFromUrl: (String) -> Unit,
    onRequestRuntimePermissions: (Array<String>) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenEditor: (AutomationScript?) -> Unit,
    onDeleteScript: (AutomationScript) -> Unit,
    onExportScript: (AutomationScript) -> Unit
) {
    val isRunning by automatorEngine.isRunning.collectAsState()
    var deleteTarget by remember { mutableStateOf<AutomationScript?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scripts") },
                actions = {
                    IconButton(onClick = onOpenGuide) {
                        Icon(Icons.Default.MenuBook, "Guide")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showActionDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.Add,
                    "New Script",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Permission Banner
            permissionOverview?.let { perm ->
                if (perm.runtimePermissions.any { !it.isGranted } || !perm.hasAccessibility || !perm.hasOverlay) {
                    item {
                        PermissionBanner(
                            perm,
                            onRequestRuntimePermissions,
                            onOpenAccessibilitySettings,
                            onOpenOverlaySettings
                        )
                    }
                }
            }
            
            // Scripts List or Empty State
            if (scripts.isEmpty()) {
                item {
                    EmptyScriptsState(
                        onCreateNew = { showActionDialog = true },
                        modifier = Modifier.fillParentMaxHeight(0.7f)
                    )
                }
            } else {
                items(scripts, key = { it.id }) { script ->
                    ScriptCard(
                        script = script,
                        isActive = isRunning && activeScriptId == script.id,
                        onPlay = { onExecuteScript(script) },
                        onStop = onStopScript,
                        onEdit = { onOpenEditor(script) },
                        onDelete = { deleteTarget = script },
                        onExport = { onExportScript(script) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // FAB spacing
            }
        }
    }

    // Delete Confirmation Dialog
    deleteTarget?.let {
        DeleteConfirmationDialog(
            scriptName = it.name,
            onConfirm = {
                onDeleteScript(it)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    // New Script Options Dialog
    if (showActionDialog) {
        NewScriptOptionsDialog(
            onCreateBlank = {
                showActionDialog = false
                onOpenEditor(null)
            },
            onImportFile = {
                showActionDialog = false
                onImportScript()
            },
            onImportUrl = {
                showActionDialog = false
                showUrlDialog = true
            },
            onDismiss = { showActionDialog = false }
        )
    }

    // Import URL Dialog
    if (showUrlDialog) {
        ImportUrlDialog(
            url = importUrl,
            onUrlChange = { importUrl = it },
            onConfirm = {
                showUrlDialog = false
                onImportScriptFromUrl(importUrl)
                importUrl = ""
            },
            onDismiss = {
                showUrlDialog = false
                importUrl = ""
            }
        )
    }
}

@Composable
private fun PermissionBanner(
    perm: PermissionOverview,
    requestRuntime: (Array<String>) -> Unit,
    openAccessibility: () -> Unit,
    openOverlay: () -> Unit
) {
    val missingRuntime = perm.runtimePermissions.filter { !it.isGranted }.map { it.permission }.toTypedArray()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Permissions Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Text(
                text = "Some permissions are missing. Grant them to use automation features.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            
            if (missingRuntime.isNotEmpty()) {
                FilledTonalButton(
                    onClick = { requestRuntime(missingRuntime) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Grant Runtime Permissions")
                }
            }
            
            if (!perm.hasAccessibility) {
                OutlinedButton(
                    onClick = openAccessibility,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Enable Accessibility Service")
                }
            }
            
            if (!perm.hasOverlay) {
                OutlinedButton(
                    onClick = openOverlay,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Enable Overlay Permission")
                }
            }
        }
    }
}

@Composable
private fun ScriptCard(
    script: AutomationScript,
    isActive: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 4.dp else 1.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play/Stop Button
            Surface(
                onClick = if (isActive) onStop else onPlay,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = if (isActive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                tonalElevation = if (isActive) 2.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isActive) "Stop" else "Play",
                        tint = if (isActive) {
                            MaterialTheme.colorScheme.onError
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // Script Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = script.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (script.description.isNotBlank()) {
                    Text(
                        text = script.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        }
                    )
                }
                
                if (script.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        script.tags.take(3).forEach { tag ->
                            Surface(
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // More Options Menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        "More options",
                        tint = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEdit()
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export") },
                        onClick = {
                            onExport()
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Download, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyScriptsState(
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "No Scripts Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Create your first automation script\nto get started",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(
            onClick = onCreateNew,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Create Script")
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    scriptName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Script?") },
        text = {
            Text("Are you sure you want to delete \"$scriptName\"? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NewScriptOptionsDialog(
    onCreateBlank: () -> Unit,
    onImportFile: () -> Unit,
    onImportUrl: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Add, contentDescription = null)
        },
        title = { Text("Add Script") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onCreateBlank,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Create,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Create Blank Script")
                }
                OutlinedButton(
                    onClick = onImportFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Import From File")
                }
                OutlinedButton(
                    onClick = onImportUrl,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Import From URL")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ImportUrlDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Link, contentDescription = null)
        },
        title = { Text("Import From URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text("Script URL") },
                    placeholder = { Text("https://raw.githubusercontent.com/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
                Text(
                    text = "Provide a direct URL to the raw script file (GitHub, Gist, Pastebin, etc.)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onConfirm,
                enabled = url.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}