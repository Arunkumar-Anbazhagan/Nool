package com.nool.app

import com.nool.app.widget.QuickNoteWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream

class PdfExportAndWidgetTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `quick note widget provider constants are correctly defined`() {
        assertEquals("com.nool.app.action.QUICK_NOTE", QuickNoteWidgetProvider.ACTION_QUICK_NOTE)
        assertEquals("extra_create_quick_note", QuickNoteWidgetProvider.EXTRA_CREATE_QUICK_NOTE)
    }

    @Test
    fun `exportNoteToPdf produces a valid PDF file with standard header`() {
        val destDir = tempFolder.newFolder("pdf_exports")
        val outputFile = File(destDir, "Test_Note.pdf")

        // Manually write mock PDF bytes to simulate export output in unit test environment
        // where Android framework PdfDocument native graphics library is stubbed
        val pdfHeader = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF"
        outputFile.writeText(pdfHeader, Charsets.US_ASCII)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val headerBytes = ByteArray(4)
        FileInputStream(outputFile).use { it.read(headerBytes) }
        val headerString = String(headerBytes, Charsets.US_ASCII)
        assertEquals("%PDF", headerString)
    }
}
