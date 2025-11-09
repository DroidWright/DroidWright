/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tas33n.droidwright.data.models.AutomationScript

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val code: String,
    val tagsJson: String, // List<String> serialized as JSON
    val lastExecuted: Long = 0,
    val isRunning: Boolean = false,
    val lastValidatedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toAutomationScript(tags: List<String>): AutomationScript {
        return AutomationScript(
            id = id,
            name = name,
            description = description,
            code = code,
            tags = tags,
            lastExecuted = lastExecuted,
            isRunning = isRunning,
            lastValidatedAt = lastValidatedAt
        )
    }
    
    companion object {
        fun fromAutomationScript(script: AutomationScript, tagsJson: String): ScriptEntity {
            return ScriptEntity(
                id = script.id,
                name = script.name,
                description = script.description,
                code = script.code,
                tagsJson = tagsJson,
                lastExecuted = script.lastExecuted,
                isRunning = script.isRunning,
                lastValidatedAt = script.lastValidatedAt,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}

