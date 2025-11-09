/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.data.models

data class AutomationScript(
    val id: String,
    val name: String,
    val description: String,
    val code: String,
    val tags: List<String> = emptyList(),
    val lastExecuted: Long = 0,
    val isRunning: Boolean = false,
    val lastValidatedAt: Long = 0
)

data class LogEntry(
    val timestamp: Long,
    val message: String,
    val level: LogLevel
)

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR, SUCCESS
}

data class PermissionStatus(
    val name: String,
    val isGranted: Boolean,
    val permission: String
)

data class AppStatus(
    val isRunning: Boolean,
    val currentTask: String,
    val progress: Float,
    val permissions: List<PermissionStatus>
)

data class PermissionOverview(
    val runtimePermissions: List<PermissionStatus>,
    val hasAccessibility: Boolean,
    val hasOverlay: Boolean,
)

data class ScriptValidationResult(
    val isValid: Boolean,
    val messages: List<ScriptValidationMessage> = emptyList()
)

data class ScriptValidationMessage(
    val message: String,
    val level: LogLevel = LogLevel.INFO,
    val line: Int? = null,
    val column: Int? = null
)

data class ScriptResult(
    val status: String, // "ok" or "error"
    val note: String? = null
)
