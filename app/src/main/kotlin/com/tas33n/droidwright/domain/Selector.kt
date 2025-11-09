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

import android.view.accessibility.AccessibilityNodeInfo

data class Selector(
    val id: String? = null,
    val text: String? = null,
    val desc: String? = null,
    val className: String? = null,
    // Additional attribute filters (Playwright/Puppeteer style)
    val checked: Boolean? = null,
    val selected: Boolean? = null,
    val enabled: Boolean? = null,
    val clickable: Boolean? = null,
    val focused: Boolean? = null,
    val visible: Boolean? = null,
    val packageName: String? = null,
)

fun Selector.toPredicate(): (AccessibilityNodeInfo) -> Boolean {
    val idMatch = id?.trim()
    val textMatch = text?.trim()?.takeIf { it.isNotEmpty() }
    val descMatch = desc?.trim()?.takeIf { it.isNotEmpty() }
    val classMatch = className?.trim()?.takeIf { it.isNotEmpty() }
    val packageMatch = packageName?.trim()?.takeIf { it.isNotEmpty() }
    
    // Check if text/desc contain regex patterns (simple heuristic: contains regex special chars)
    val textIsRegex = textMatch != null && textMatch.contains(Regex("""[.*+?^${'$'}|\\[\\]()]"""))
    val descIsRegex = descMatch != null && descMatch.contains(Regex("""[.*+?^${'$'}|\\[\\]()]"""))
    
    val textRegex = if (textIsRegex) textMatch?.toRegex(RegexOption.IGNORE_CASE) else null
    val descRegex = if (descIsRegex) descMatch?.toRegex(RegexOption.IGNORE_CASE) else null
    
    return { node ->
        val nodeResourceId = node.viewIdResourceName?.trim()
        val textValue = node.text?.toString()?.trim().orEmpty()
        val descValue = node.contentDescription?.toString()?.trim().orEmpty()
        val nodeClassName = node.className?.toString()?.trim()
        val nodePackageName = node.packageName?.toString()?.trim()
        
        // Resource ID matching: support both full ID and resource name
        val idMatches = idMatch == null || when {
            nodeResourceId == null -> false
            idMatch.equals(nodeResourceId, ignoreCase = false) -> true // Exact match (case-sensitive for IDs)
            idMatch.contains(":id/") && nodeResourceId.contains(":id/") -> {
                // Match full resource ID
                idMatch.equals(nodeResourceId, ignoreCase = false)
            }
            else -> {
                // Match resource name part (e.g., "row_feed_button_like" matches "com.instagram.android:id/row_feed_button_like")
                val resourceName = idMatch.substringAfterLast("/").substringAfterLast(":")
                nodeResourceId.endsWith(":id/$resourceName", ignoreCase = false) || 
                nodeResourceId.endsWith(":$resourceName", ignoreCase = false)
            }
        }
        
        // Text matching: exact match (case-insensitive) by default, regex if pattern detected
        val textMatches = textMatch == null || when {
            textValue.isEmpty() -> false
            textRegex != null -> textRegex.containsMatchIn(textValue) // Regex match
            else -> textMatch.equals(textValue, ignoreCase = true) // Exact match (case-insensitive)
        }
        
        // Content description matching (accessibility ID): exact match (case-insensitive) by default, regex if pattern detected
        val descMatches = descMatch == null || when {
            descValue.isEmpty() -> false
            descRegex != null -> descRegex.containsMatchIn(descValue) // Regex match
            else -> descMatch.equals(descValue, ignoreCase = true) // Exact match (case-insensitive)
        }
        
        // Class name matching - support both full class name and simple name
        val classMatches = classMatch == null || when {
            nodeClassName == null -> false
            classMatch == nodeClassName -> true // Exact match
            classMatch.contains(".") -> classMatch == nodeClassName // Full class name
            else -> nodeClassName.endsWith(".$classMatch") || nodeClassName == classMatch // Simple class name
        }
        
        // Package name matching
        val packageMatches = packageMatch == null || when {
            nodePackageName == null -> false
            else -> packageMatch.equals(nodePackageName, ignoreCase = true)
        }
        
        // Attribute filters (Playwright/Puppeteer style)
        val checkedMatches = checked == null || node.isChecked == checked
        val selectedMatches = selected == null || node.isSelected == selected
        val enabledMatches = enabled == null || node.isEnabled == enabled
        val clickableMatches = clickable == null || node.isClickable == clickable
        val focusedMatches = focused == null || node.isFocused == focused
        val visibleMatches = visible == null || node.isVisibleToUser == visible
        
        idMatches && textMatches && descMatches && classMatches && packageMatches &&
        checkedMatches && selectedMatches && enabledMatches && clickableMatches && 
        focusedMatches && visibleMatches
    }
}
