/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.util

import android.view.accessibility.AccessibilityNodeInfo
import java.lang.reflect.Method

/**
 * Replaces direct calls to deprecated [AccessibilityNodeInfo.recycle].
 */
fun AccessibilityNodeInfo.release() {
    if (invokeCloseIfAvailable()) return
    @Suppress("DEPRECATION")
    recycle()
}

private val closeMethod: Method? by lazy {
    runCatching {
        AccessibilityNodeInfo::class.java.getMethod("close")
    }.getOrNull()
}

private fun AccessibilityNodeInfo.invokeCloseIfAvailable(): Boolean {
    val method = closeMethod ?: return false
    return try {
        method.invoke(this)
        true
    } catch (_: Exception) {
        false
    }
}
