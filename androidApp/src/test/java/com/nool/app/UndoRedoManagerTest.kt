package com.nool.app

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.nool.app.ui.editor.UndoRedoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoRedoManagerTest {

    @Test
    fun `test undo and redo flow`() {
        val manager = UndoRedoManager()
        val v1 = TextFieldValue("Hello", TextRange(5))
        manager.reset(v1)

        assertFalse(manager.canUndo)
        assertFalse(manager.canRedo)

        val v2 = TextFieldValue("Hello World", TextRange(11))
        manager.push(v2)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)

        val undone = manager.undo()
        assertEquals("Hello", undone?.text)
        assertFalse(manager.canUndo)
        assertTrue(manager.canRedo)

        val redone = manager.redo()
        assertEquals("Hello World", redone?.text)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)
    }

    @Test
    fun `test undo when stack is empty returns null`() {
        val manager = UndoRedoManager()
        manager.reset(TextFieldValue("Initial", TextRange(0)))
        assertNull(manager.undo())
    }
}
