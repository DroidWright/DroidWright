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

object ScriptMetadataParser {
    private val entryRegex = Regex("""^//\s*@([A-Za-z0-9_\-]+)\s+(.*)$""")

    fun parse(source: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        var insideHeader = false
        source.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.equals("// ==DroidScript==", ignoreCase = true) -> {
                    insideHeader = true
                    return@forEach
                }
                line.equals("// ==/DroidScript==", ignoreCase = true) -> {
                    insideHeader = false
                    return@forEach
                }
                insideHeader && line.startsWith("// @") -> {
                    val match = entryRegex.find(line)
                    if (match != null) {
                        val key = match.groupValues.getOrNull(1)?.trim().orEmpty()
                        val value = match.groupValues.getOrNull(2)?.trim().orEmpty()
                        if (key.isNotEmpty() && value.isNotEmpty()) {
                            metadata[key] = value
                        }
                    }
                }
            }
        }
        return metadata
    }
}
