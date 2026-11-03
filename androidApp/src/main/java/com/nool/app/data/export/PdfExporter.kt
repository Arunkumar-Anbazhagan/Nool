package com.nool.app.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points
    private const val MARGIN_HORIZONTAL = 48f
    private const val MARGIN_TOP = 56f
    private const val MARGIN_BOTTOM = 56f

    /**
     * Exports a note's title and markdown content to a paginated PDF document.
     */
    fun exportNoteToPdf(
        context: Context,
        title: String,
        content: String,
        destinationDir: File = File(context.cacheDir, "exports")
    ): Result<File> = runCatching {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "Untitled" }
        val outputFile = File(destinationDir, "$sanitizedTitle.pdf")

        val pdfDocument = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val usableWidth = PAGE_WIDTH - (MARGIN_HORIZONTAL * 2)
        val usableBottom = PAGE_HEIGHT - MARGIN_BOTTOM
        val bodyLineHeight = bodyPaint.fontSpacing * 1.35f

        // Break content into lines that fit within usableWidth
        val wrappedLines = mutableListOf<String>()
        for (paragraph in content.lines()) {
            if (paragraph.isEmpty()) {
                wrappedLines.add("")
            } else {
                val words = paragraph.split(" ")
                val currentLine = StringBuilder()
                for (word in words) {
                    val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (bodyPaint.measureText(candidate) <= usableWidth) {
                        currentLine.setLength(0)
                        currentLine.append(candidate)
                    } else {
                        if (currentLine.isNotEmpty()) {
                            wrappedLines.add(currentLine.toString())
                            currentLine.setLength(0)
                        }
                        currentLine.append(word)
                    }
                }
                if (currentLine.isNotEmpty()) {
                    wrappedLines.add(currentLine.toString())
                }
            }
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN_TOP

        // Draw Title
        canvas.drawText(title.ifBlank { "Untitled Note" }, MARGIN_HORIZONTAL, y + 20f, titlePaint)
        y += 40f

        // Draw divider line under title
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_HORIZONTAL, y, PAGE_WIDTH - MARGIN_HORIZONTAL, y, linePaint)
        y += 24f

        for (line in wrappedLines) {
            if (y + bodyLineHeight > usableBottom) {
                // Draw footer with page number
                val footerText = "Page $pageNumber • Exported with Nool"
                canvas.drawText(footerText, MARGIN_HORIZONTAL, PAGE_HEIGHT - 30f, footerPaint)

                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN_TOP
            }

            if (line.isNotEmpty()) {
                canvas.drawText(line, MARGIN_HORIZONTAL, y + bodyPaint.textSize, bodyPaint)
            }
            y += bodyLineHeight
        }

        // Draw footer on the final page
        val footerText = "Page $pageNumber • Exported with Nool"
        canvas.drawText(footerText, MARGIN_HORIZONTAL, PAGE_HEIGHT - 30f, footerPaint)
        pdfDocument.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        outputFile
    }

    /**
     * Launches an Android ACTION_SEND intent to share or view the exported PDF.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, pdfFile.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share PDF Note")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
