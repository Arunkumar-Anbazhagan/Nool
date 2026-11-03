package com.nool.app.ui.editor.table

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

enum class TableColumnAlignment {
    LEFT,
    CENTER,
    RIGHT
}

@Composable
fun VisualTableEditorDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onInsertTable: (markdownTable: String) -> Unit
) {
    if (!visible) return

    var colCount by remember { mutableIntStateOf(3) }
    var rowCount by remember { mutableIntStateOf(3) }

    // Table headers and data rows
    val headers = remember { mutableStateListOf("Col 1", "Col 2", "Col 3") }
    val alignments = remember { mutableStateListOf(TableColumnAlignment.LEFT, TableColumnAlignment.CENTER, TableColumnAlignment.RIGHT) }
    val rows = remember {
        mutableStateListOf(
            mutableStateListOf("A1", "B1", "C1"),
            mutableStateListOf("A2", "B2", "C2")
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Visual Table Editor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configure columns, rows, and alignment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: add column / add row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            colCount++
                            headers.add("Col $colCount")
                            alignments.add(TableColumnAlignment.LEFT)
                            rows.forEach { it.add("") }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Column", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = {
                            rowCount++
                            val newRow = mutableStateListOf<String>()
                            for (c in 0 until headers.size) {
                                newRow.add("")
                            }
                            rows.add(newRow)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Row", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Table Grid
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Headers row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            headers.forEachIndexed { colIdx, headerText ->
                                Column(modifier = Modifier.width(130.dp)) {
                                    OutlinedTextField(
                                        value = headerText,
                                        onValueChange = { headers[colIdx] = it },
                                        label = { Text("Header ${colIdx + 1}", style = MaterialTheme.typography.labelSmall) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    // Alignment toggle
                                    IconButton(
                                        onClick = {
                                            alignments[colIdx] = when (alignments[colIdx]) {
                                                TableColumnAlignment.LEFT -> TableColumnAlignment.CENTER
                                                TableColumnAlignment.CENTER -> TableColumnAlignment.RIGHT
                                                TableColumnAlignment.RIGHT -> TableColumnAlignment.LEFT
                                            }
                                        },
                                        modifier = Modifier.size(28.dp).align(Alignment.CenterHorizontally)
                                    ) {
                                        val icon = when (alignments[colIdx]) {
                                            TableColumnAlignment.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                                            TableColumnAlignment.CENTER -> Icons.Default.FormatAlignCenter
                                            TableColumnAlignment.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
                                        }
                                        Icon(icon, contentDescription = "Align", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Data rows
                        rows.forEachIndexed { rowIdx, rowCells ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowCells.forEachIndexed { colIdx, cellValue ->
                                    OutlinedTextField(
                                        value = cellValue,
                                        onValueChange = { rowCells[colIdx] = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(130.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val markdown = buildMarkdownTable(headers, alignments, rows)
                            onInsertTable(markdown)
                            onDismiss()
                        }
                    ) {
                        Text("Insert Table")
                    }
                }
            }
        }
    }
}

fun buildMarkdownTable(
    headers: List<String>,
    alignments: List<TableColumnAlignment>,
    rows: List<List<String>>
): String {
    val sb = StringBuilder()
    // Header
    sb.append("| ")
    headers.forEach { h -> sb.append(h.ifBlank { " " }).append(" | ") }
    sb.append("\n")

    // Separator / Alignment
    sb.append("| ")
    alignments.forEach { a ->
        val sep = when (a) {
            TableColumnAlignment.LEFT -> ":---"
            TableColumnAlignment.CENTER -> ":---:"
            TableColumnAlignment.RIGHT -> "---:"
        }
        sb.append(sep).append(" | ")
    }
    sb.append("\n")

    // Rows
    rows.forEach { r ->
        sb.append("| ")
        for (i in headers.indices) {
            val cell = if (i < r.size) r[i] else ""
            sb.append(cell.ifBlank { " " }).append(" | ")
        }
        sb.append("\n")
    }

    return sb.toString()
}
