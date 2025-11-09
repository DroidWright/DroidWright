/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.tas33n.droidwright.data.models.PermissionOverview
import com.tas33n.droidwright.data.models.PermissionStatus
import com.tas33n.droidwright.service.AutomationAccessibilityService

object PermissionRepository {

    /**
     * Required permissions list.
     * 
     * Note: On Android 11+ (API 30+), READ_EXTERNAL_STORAGE is not needed when using
     * Storage Access Framework (SAF) for file access. The app uses GetContent() for
     * file imports, which doesn't require storage permissions.
     * 
     * On Android 10 and below, WRITE_EXTERNAL_STORAGE might be needed for some use cases,
     * but since we use SAF, we don't actually require it for core functionality.
     * However, we keep it for backward compatibility in case of future features.
     */
    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf("android.permission.INTERNET")
        // On Android 11+ (API 30+), storage permissions work differently:
        // - READ_EXTERNAL_STORAGE may show as not granted even when not needed
        // - Apps using SAF (Storage Access Framework) don't need this permission
        // - Since we use GetContent() for imports, we don't need storage permissions
        // 
        // We only check storage permissions on Android 10 and below for backward compatibility
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Android 10 and below - check WRITE_EXTERNAL_STORAGE for backward compatibility
            // Note: This might not be strictly needed since we use SAF, but it's here
            // for apps that might need it for other features
            permissions.add("android.permission.WRITE_EXTERNAL_STORAGE")
        }
        // On Android 11+, we don't check storage permissions because:
        // 1. We use SAF (GetContent()) which doesn't require them
        // 2. The permission check is unreliable and may show false negatives on emulators
        return permissions
    }

    fun getPermissionStatuses(context: Context): List<PermissionStatus> {
        return getRequiredPermissions().map { permission ->
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED

            PermissionStatus(
                name = permission.substringAfterLast("."),
                isGranted = isGranted,
                permission = permission
            )
        }
    }

    fun hasAccessibility(context: Context): Boolean {
        val serviceName = AutomationAccessibilityService::class.java.canonicalName ?: return false
        val serviceId = "${context.packageName}/$serviceName"
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServicesSetting)
        while (splitter.hasNext()) {
            if (splitter.next().equals(serviceId, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun hasFloatingWindowPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }
    }


    fun getPermissionOverview(context: Context): PermissionOverview {
        val runtimePermissions = getPermissionStatuses(context)
        val accessibility = hasAccessibility(context)
        val overlay = hasFloatingWindowPermission(context)
        return PermissionOverview(
            runtimePermissions = runtimePermissions,
            hasAccessibility = accessibility,
            hasOverlay = overlay
        )
    }

    fun getMissingRuntimePermissions(context: Context): List<String> {
        return getPermissionStatuses(context)
            .filterNot { it.isGranted }
            .map { it.permission }
    }
}
