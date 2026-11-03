package com.nool.app.ui.editor

import androidx.compose.ui.text.input.TextFieldValue

class UndoRedoManager(
    private val maxHistory: Int = 100
) {
    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var current: TextFieldValue? = null

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun reset(initial: TextFieldValue) {
        undoStack.clear()
        redoStack.clear()
        current = initial
    }

    fun push(value: TextFieldValue) {
        val prev = current
        if (prev != null && prev.text == value.text) {
            // Only selection changed; update current without push
            current = value
            return
        }

        if (prev != null) {
            undoStack.addLast(prev)
            if (undoStack.size > maxHistory) {
                undoStack.removeFirst()
            }
        }
        redoStack.clear()
        current = value
    }

    fun undo(): TextFieldValue? {
        if (!canUndo) return null
        val cur = current ?: return null
        redoStack.addLast(cur)
        val target = undoStack.removeLast()
        current = target
        return target
    }

    fun redo(): TextFieldValue? {
        if (!canRedo) return null
        val cur = current ?: return null
        undoStack.addLast(cur)
        val target = redoStack.removeLast()
        current = target
        return target
    }
}
