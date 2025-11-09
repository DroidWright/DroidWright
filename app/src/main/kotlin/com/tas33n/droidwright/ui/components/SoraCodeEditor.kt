/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tas33n.droidwright.R
import com.tas33n.droidwright.editor.SoraEditorInitializer
import com.tas33n.droidwright.editor.SoraTheme
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways

@Composable
fun SoraCodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    languageScopeName: String = "source.js",
    controller: SoraEditorController? = null
) {
    val selectedTheme = if (isSystemInDarkTheme()) SoraTheme.Dark else SoraTheme.Light
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestValue by rememberUpdatedState(value)
    val resolvedController = controller ?: rememberSoraEditorController()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SoraEditorInitializer.ensureInitialized(ctx.applicationContext, selectedTheme)
            val editor = CodeEditor(ctx).apply {
                typefaceText = Typeface.MONOSPACE
                setTextSize(MIN_TEXT_SIZE_SP)
                setLineSpacing(2f, 1.1f)
                setWordwrap(true)
                nonPrintablePaintingFlags =
                    CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or
                        CodeEditor.FLAG_DRAW_LINE_SEPARATOR or
                        CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION
                props.apply {
                    stickyScroll = true
                    symbolPairAutoCompletion = true
                    overScrollEnabled = false
                }
                colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                setEditorLanguage(TextMateLanguage.create(languageScopeName, true))
                setText(value)
            }

            var binding: EditorBinding? = null
            val subscription = editor.subscribeAlways<ContentChangeEvent> {
                val holder = binding ?: return@subscribeAlways
                val newText = editor.text.toString()
                holder.lastKnownText = newText
                if (newText != latestValue) {
                    latestOnValueChange(newText)
                }
                resolvedController.refreshCapabilities(editor)
            }
            resolvedController.bindEditor(editor)
            resolvedController.refreshCapabilities(editor)
            binding = EditorBinding(subscription, selectedTheme, value)
            editor.setTag(R.id.tag_sora_editor_binding, binding)
            editor
        },
        update = { editor ->
            val binding = editor.getTag(R.id.tag_sora_editor_binding) as? EditorBinding
            if (binding != null && binding.appliedTheme != selectedTheme) {
                SoraEditorInitializer.ensureInitialized(editor.context.applicationContext, selectedTheme)
                editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                binding.appliedTheme = selectedTheme
            }

            if (binding != null && binding.lastKnownText != value) {
                editor.setText(value)
                val lastLine = editor.text.lineCount - 1
                val lastColumn = editor.text.getColumnCount(lastLine)
                editor.setSelection(lastLine, lastColumn)
                binding.lastKnownText = value
            }
            resolvedController.refreshCapabilities(editor)
        },
        onRelease = { editor ->
            (editor.getTag(R.id.tag_sora_editor_binding) as? EditorBinding)?.let { binding ->
                binding.subscription.unsubscribe()
            }
            resolvedController.unbindEditor(editor)
            editor.release()
        }
    )
}

private data class EditorBinding(
    val subscription: SubscriptionReceipt<ContentChangeEvent>,
    var appliedTheme: SoraTheme,
    var lastKnownText: String
)

private const val MIN_TEXT_SIZE_SP = 12f
