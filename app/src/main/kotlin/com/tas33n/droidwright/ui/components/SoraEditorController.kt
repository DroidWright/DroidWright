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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.rosemoe.sora.widget.CodeEditor

class SoraEditorController internal constructor() {
    private var editor: CodeEditor? = null
    private val _canUndo = mutableStateOf(false)
    private val _canRedo = mutableStateOf(false)

    val canUndo: State<Boolean> get() = _canUndo
    val canRedo: State<Boolean> get() = _canRedo

    internal fun bindEditor(newEditor: CodeEditor) {
        editor = newEditor
        refreshCapabilities(newEditor)
    }

    internal fun unbindEditor(editorToClear: CodeEditor) {
        if (editor == editorToClear) {
            editor = null
            _canUndo.value = false
            _canRedo.value = false
        }
    }

    internal fun refreshCapabilities(targetEditor: CodeEditor? = editor) {
        val current = targetEditor ?: return
        _canUndo.value = current.canUndo()
        _canRedo.value = current.canRedo()
    }

    fun undo() {
        editor?.let {
            it.undo()
            refreshCapabilities(it)
        }
    }

    fun redo() {
        editor?.let {
            it.redo()
            refreshCapabilities(it)
        }
    }
}

@Composable
fun rememberSoraEditorController(): SoraEditorController = remember { SoraEditorController() }
