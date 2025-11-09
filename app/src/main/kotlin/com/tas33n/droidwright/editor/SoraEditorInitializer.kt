/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.editor

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource

enum class SoraTheme(val assetName: String, val isDark: Boolean) {
    Dark("darcula", true),
    Light("quietlight", false)
}

/**
 * One-time bootstrap for TextMate grammar + theme registries that back Sora editor.
 */
object SoraEditorInitializer {
    private val lock = Any()
    @Volatile
    private var baseInitialized = false

    fun ensureInitialized(context: Context, theme: SoraTheme) {
        ensureBaseSetup(context.applicationContext)
        ThemeRegistry.getInstance().setTheme(theme.assetName)
    }

    private fun ensureBaseSetup(appContext: Context) {
        if (baseInitialized) return
        synchronized(lock) {
            if (baseInitialized) return
            val providerRegistry = FileProviderRegistry.getInstance()
            providerRegistry.addFileProvider(AssetsFileResolver(appContext.assets))
            loadTheme(SoraTheme.Dark)
            loadTheme(SoraTheme.Light)
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            baseInitialized = true
        }
    }

    private fun loadTheme(theme: SoraTheme) {
        val themePath = "textmate/${theme.assetName}.json"
        val inputStream = FileProviderRegistry.getInstance().tryGetInputStream(themePath)
            ?: throw IllegalStateException("Unable to locate TextMate theme asset: $themePath")
        ThemeRegistry.getInstance().loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(inputStream, themePath, null),
                theme.assetName
            ).apply {
                isDark = theme.isDark
            }
        )
    }
}
