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

/**
 * Parser for different selector formats:
 * - UiAutomator: new UiSelector().resourceId("...")
 * - XPath: //android.widget.Button[@content-desc="Liked"]
 */
object SelectorParser {
    
    /**
     * Detects selector format and converts to standard Selector format
     */
    fun parseSelector(input: String): Map<String, String> {
        return when {
            input.trim().startsWith("new UiSelector()") -> parseUiAutomator(input)
            input.trim().startsWith("//") || input.trim().startsWith("/") -> parseXPath(input)
            else -> emptyMap() // Return empty, will be handled as regular selector
        }
    }
    
    /**
     * Parses UiAutomator selector expressions
     * Example: new UiSelector().resourceId("com.instagram.android:id/row_feed_button_like")
     */
    private fun parseUiAutomator(expression: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        // Extract resourceId
        val resourceIdMatch = Regex("""\.resourceId\(["']([^"']+)["']\)""").find(expression)
        resourceIdMatch?.groupValues?.get(1)?.let { result["id"] = it }
        
        // Extract text
        val textMatch = Regex("""\.text\(["']([^"']+)["']\)""").find(expression)
        textMatch?.groupValues?.get(1)?.let { result["text"] = it }
        
        // Extract description (content-desc)
        val descMatch = Regex("""\.description\(["']([^"']+)["']\)""").find(expression)
        descMatch?.groupValues?.get(1)?.let { result["desc"] = it }
        
        // Extract className
        val classNameMatch = Regex("""\.className\(["']([^"']+)["']\)""").find(expression)
        classNameMatch?.groupValues?.get(1)?.let { result["className"] = it }
        
        // Extract packageName
        val packageMatch = Regex("""\.packageName\(["']([^"']+)["']\)""").find(expression)
        packageMatch?.groupValues?.get(1)?.let { result["package"] = it }
        
        // Extract checked
        val checkedMatch = Regex("""\.checked\(([^)]+)\)""").find(expression)
        checkedMatch?.groupValues?.get(1)?.let { result["checked"] = it.trim() }
        
        // Extract enabled
        val enabledMatch = Regex("""\.enabled\(([^)]+)\)""").find(expression)
        enabledMatch?.groupValues?.get(1)?.let { result["enabled"] = it.trim() }
        
        // Extract clickable
        val clickableMatch = Regex("""\.clickable\(([^)]+)\)""").find(expression)
        clickableMatch?.groupValues?.get(1)?.let { result["clickable"] = it.trim() }
        
        // Extract selected
        val selectedMatch = Regex("""\.selected\(([^)]+)\)""").find(expression)
        selectedMatch?.groupValues?.get(1)?.let { result["selected"] = it.trim() }
        
        return result
    }

    
//    /**
//     * Parses XPath selector expressions
//     * Examples:
//     * - //android.widget.Button[@content-desc="Liked"]
//     * - //*[@resource-id="com.instagram.android:id/row_feed_button_like"]
//     * - //android.widget.Button[@text="Like"]
//     **/

    private fun parseXPath(xpath: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        // Extract element name (e.g., "android.widget.Button" from "//android.widget.Button[...]")
        val elementMatch = Regex("""//([^[@]+)""").find(xpath)
        elementMatch?.groupValues?.get(1)?.let { className ->
            // Remove wildcard, use actual class name
            if (className != "*") {
                result["className"] = className.trim()
            }
        }
        
        // Extract attributes from [@attribute="value"] format
        val attributePattern = Regex("""@(\w+(?:-\w+)*)=["']([^"']+)["']""")
        attributePattern.findAll(xpath).forEach { match ->
            val attributeName = match.groupValues[1]
            val attributeValue = match.groupValues[2]
            
            when (attributeName.lowercase()) {
                "resource-id", "resourceid" -> result["id"] = attributeValue
                "content-desc", "contentdesc" -> result["desc"] = attributeValue
                "text" -> result["text"] = attributeValue
                "class" -> result["className"] = attributeValue
                "package" -> result["package"] = attributeValue
                "checked" -> result["checked"] = attributeValue
                "selected" -> result["selected"] = attributeValue
                "enabled" -> result["enabled"] = attributeValue
                "clickable" -> result["clickable"] = attributeValue
                "focused" -> result["focused"] = attributeValue
                "visible" -> result["visible"] = attributeValue
            }
        }
        
        return result
    }
    
    /**
     * Checks if a string looks like a UiAutomator or XPath selector
     */
    fun isSpecialSelector(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.startsWith("new UiSelector()") || 
               trimmed.startsWith("//") || 
               trimmed.startsWith("/")
    }
}
