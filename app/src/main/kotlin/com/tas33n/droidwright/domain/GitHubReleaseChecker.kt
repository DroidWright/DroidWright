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

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

object GitHubReleaseChecker {
    private const val TAG = "GitHubReleaseChecker"
    private const val REPO_OWNER = "tas33n"
    private const val REPO_NAME = "droidwright"
    private const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases"
    
    /**
     * Compares two version strings
     * Returns: positive if newVersion > currentVersion, negative if newVersion < currentVersion, 0 if equal
     */
    fun compareVersions(currentVersion: String, newVersion: String): Int {
        // Remove "beta", "alpha", etc. suffixes and split by dots
        val currentParts = currentVersion.replace(Regex("[^0-9.]"), "").split(".").mapNotNull { it.toIntOrNull() }
        val newParts = newVersion.replace(Regex("[^0-9.]"), "").split(".").mapNotNull { it.toIntOrNull() }
        
        val maxLength = maxOf(currentParts.size, newParts.size)
        
        for (i in 0 until maxLength) {
            val current = currentParts.getOrElse(i) { 0 }
            val new = newParts.getOrElse(i) { 0 }
            
            when {
                new > current -> return 1
                new < current -> return -1
            }
        }
        
        return 0
    }
    
    suspend fun getLatestRelease(): Result<GitHubRelease> = withContext(Dispatchers.IO) {
        try {
            // Reduce timeouts to prevent long blocking
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000 // Reduced from 10000
            connection.readTimeout = 5000 // Reduced from 10000
            connection.instanceFollowRedirects = true
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Failed to fetch releases: HTTP $responseCode")
                return@withContext Result.failure(Exception("HTTP $responseCode"))
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val releases = JSONArray(response)
            
            if (releases.length() == 0) {
                return@withContext Result.failure(Exception("No releases found"))
            }
            
            // Get the latest release (first in the array)
            val latestRelease = releases.getJSONObject(0)
            val release = parseRelease(latestRelease)
            
            Log.d(TAG, "Latest release: ${release.tagName}")
            Result.success(release)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Timeout fetching latest release", e)
            Result.failure(Exception("Connection timeout"))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest release", e)
            Result.failure(e)
        }
    }
    
    private fun parseRelease(json: JSONObject): GitHubRelease {
        val tagName = json.getString("tag_name")
        val name = json.getString("name")
        val body = json.optString("body", "")
        val publishedAt = json.getString("published_at")
        val htmlUrl = json.getString("html_url")
        
        val assetsArray = json.optJSONArray("assets") ?: JSONArray()
        val assets = mutableListOf<ReleaseAsset>()
        
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            assets.add(
                ReleaseAsset(
                    name = asset.getString("name"),
                    downloadUrl = asset.getString("browser_download_url"),
                    size = asset.getLong("size")
                )
            )
        }
        
        return GitHubRelease(
            tagName = tagName,
            name = name,
            body = body,
            publishedAt = publishedAt,
            htmlUrl = htmlUrl,
            assets = assets
        )
    }
    
    /**
     * Check if a new version is available
     * @param currentVersion Current app version (e.g., "1.0.0-beta")
     * @return Result containing the latest release if a newer version is available, or null if up to date
     */
    suspend fun checkForUpdate(currentVersion: String): Result<GitHubRelease?> = withContext(Dispatchers.IO) {
        try {
            val latestReleaseResult = getLatestRelease()
            latestReleaseResult.fold(
                onSuccess = { latestRelease ->
                    // Compare versions
                    val comparison = compareVersions(currentVersion, latestRelease.tagName)
                    if (comparison > 0) {
                        // New version available
                        Result.success(latestRelease)
                    } else {
                        // Up to date
                        Result.success(null)
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update", e)
            Result.failure(e)
        }
    }
}

