package com.nool.app.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onHeading: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onStrikethrough: () -> Unit,
    onChecklist: () -> Unit,
    onBulletList: () -> Unit,
    onNumberedList: () -> Unit,
    onInlineCode: () -> Unit,
    onCodeBlock: () -> Unit,
    onBlockquote: () -> Unit,
    onTable: () -> Unit,
    onLink: () -> Unit,
    onWikiLink: () -> Unit,
    onAttachImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
        ) {
            // Undo / Redo
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp))

            // Headings & Inline Style
            IconButton(onClick = onHeading, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Title, contentDescription = "Heading", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onBold, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onItalic, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onStrikethrough, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatStrikethrough, contentDescription = "Strikethrough", modifier = Modifier.size(20.dp))
            }

            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp))

            // Lists & Checklists
            IconButton(onClick = onChecklist, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.CheckBox, contentDescription = "Checklist", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onBulletList, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Bullet List", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onNumberedList, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", modifier = Modifier.size(20.dp))
            }

            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp))

            // Quotes & Code
            IconButton(onClick = onBlockquote, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatQuote, contentDescription = "Blockquote", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onInlineCode, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Code, contentDescription = "Inline Code", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onCodeBlock, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.DataArray, contentDescription = "Code Block", modifier = Modifier.size(20.dp))
            }

            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp))

            // Tables, Links, and Attachments
            IconButton(onClick = onTable, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.TableChart, contentDescription = "Table", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onLink, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Link, contentDescription = "Link", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onWikiLink, modifier = Modifier.size(36.dp)) {
                androidx.compose.material3.Text(
                    text = "[[ ]]",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            IconButton(onClick = onAttachImage, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Image, contentDescription = "Attach Image", modifier = Modifier.size(20.dp))
            }
        }
    }
}
