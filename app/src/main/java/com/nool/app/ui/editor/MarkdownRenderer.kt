package com.nool.app.ui.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.CustomBlock
import org.commonmark.node.CustomNode
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
import java.io.File

@Composable
fun MarkdownRenderer(
    markdown: String,
    vaultRoot: File,
    modifier: Modifier = Modifier,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)? = null,
    onLinkClick: ((url: String) -> Unit)? = null,
    onWikiLinkClick: ((target: String) -> Unit)? = null
) {
    val extensions = listOf(
        TablesExtension.create(),
        StrikethroughExtension.create()
    )
    val parser = Parser.builder().extensions(extensions).build()
    val document = parser.parse(markdown)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var child: Node? = document.firstChild
        while (child != null) {
            RenderBlockNode(
                node = child,
                vaultRoot = vaultRoot,
                markdown = markdown,
                onToggleTask = onToggleTask,
                onLinkClick = onLinkClick,
                onWikiLinkClick = onWikiLinkClick
            )
            child = child.next
        }
    }
}

@Composable
private fun RenderBlockNode(
    node: Node,
    vaultRoot: File,
    markdown: String,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    when (node) {
        is Heading -> {
            RenderHeading(node, onLinkClick, onWikiLinkClick)
        }
        is Paragraph -> {
            RenderParagraph(node, vaultRoot, onToggleTask, markdown, onLinkClick, onWikiLinkClick)
        }
        is BlockQuote -> {
            RenderBlockQuote(node, vaultRoot, markdown, onToggleTask, onLinkClick, onWikiLinkClick)
        }
        is FencedCodeBlock -> {
            RenderCodeBlock(node.literal, node.info)
        }
        is IndentedCodeBlock -> {
            RenderCodeBlock(node.literal, null)
        }
        is BulletList -> {
            RenderBulletList(node, vaultRoot, markdown, onToggleTask, onLinkClick, onWikiLinkClick)
        }
        is OrderedList -> {
            RenderOrderedList(node, vaultRoot, markdown, onToggleTask, onLinkClick, onWikiLinkClick)
        }
        is ThematicBreak -> {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        is CustomBlock -> {
            if (node is TableBlock) {
                RenderTable(node, onLinkClick, onWikiLinkClick)
            }
        }
        else -> {
            // Fallback for unhandled block types
            Text(
                text = node.toString(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RenderHeading(
    heading: Heading,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    val text = buildInlineAnnotatedString(heading, onLinkClick != null || onWikiLinkClick != null)
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        when (heading.level) {
            1 -> {
                ClickableText(
                    text = text,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    onClick = { offset -> handleLinkClick(offset, text, context, onLinkClick, onWikiLinkClick) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            }
            2 -> {
                ClickableText(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    onClick = { offset -> handleLinkClick(offset, text, context, onLinkClick, onWikiLinkClick) }
                )
            }
            3 -> {
                ClickableText(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    onClick = { offset -> handleLinkClick(offset, text, context, onLinkClick, onWikiLinkClick) }
                )
            }
            else -> {
                ClickableText(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    ),
                    onClick = { offset -> handleLinkClick(offset, text, context, onLinkClick, onWikiLinkClick) }
                )
            }
        }
    }
}

@Composable
private fun RenderParagraph(
    paragraph: Paragraph,
    vaultRoot: File,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    markdown: String,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    val plain = extractPlainText(paragraph).trim()
    val embedMatch = Regex("""^!\[\[([^\]\n]+)\]\]$""").matchEntire(plain)
    if (embedMatch != null) {
        val target = embedMatch.groupValues[1].trim()
        val isImage = target.endsWith(".png", true) || target.endsWith(".jpg", true) ||
                      target.endsWith(".jpeg", true) || target.endsWith(".webp", true) ||
                      target.endsWith(".gif", true) || target.startsWith("attachments/")

        if (isImage) {
            val file = if (target.startsWith("attachments/")) File(vaultRoot, target) else File(vaultRoot, "attachments/$target")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(if (file.exists()) file else File(vaultRoot, target))
                        .crossfade(true)
                        .build(),
                    contentDescription = target,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                )
            }
            return
        } else {
            val noteTarget = target.substringBefore("#").substringBefore("|").trim()
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onWikiLinkClick?.invoke(noteTarget) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = noteTarget,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Embedded note • Tap to open",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            return
        }
    }

    // Check if the paragraph contains an image as its primary child
    val firstChild = paragraph.firstChild
    if (firstChild is Image) {
        RenderImage(firstChild, vaultRoot)
        return
    }

    val context = LocalContext.current
    val annotatedString = buildInlineAnnotatedString(paragraph, onLinkClick != null || onWikiLinkClick != null)

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 24.sp
        ),
        onClick = { offset -> handleLinkClick(offset, annotatedString, context, onLinkClick, onWikiLinkClick) }
    )
}

@Composable
private fun RenderBlockQuote(
    blockQuote: BlockQuote,
    vaultRoot: File,
    markdown: String,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            var child: Node? = blockQuote.firstChild
            while (child != null) {
                RenderBlockNode(child, vaultRoot, markdown, onToggleTask, onLinkClick, onWikiLinkClick)
                child = child.next
            }
        }
    }
}

@Composable
private fun RenderCodeBlock(code: String, language: String?) {
    val clipboard = LocalClipboardManager.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language?.ifBlank { "code" } ?: "code",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code.trimEnd()))
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    text = code.trimEnd(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun RenderBulletList(
    bulletList: BulletList,
    vaultRoot: File,
    markdown: String,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var item: Node? = bulletList.firstChild
        while (item != null) {
            if (item is ListItem) {
                RenderListItem(item, bullet = "•", vaultRoot, markdown, onToggleTask, onLinkClick, onWikiLinkClick)
            }
            item = item.next
        }
    }
}

@Composable
private fun RenderOrderedList(
    orderedList: OrderedList,
    vaultRoot: File,
    markdown: String,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var item: Node? = orderedList.firstChild
        var index = orderedList.startNumber
        while (item != null) {
            if (item is ListItem) {
                RenderListItem(item, bullet = "$index.", vaultRoot, markdown, onToggleTask, onLinkClick, onWikiLinkClick)
                index++
            }
            item = item.next
        }
    }
}

@Composable
private fun RenderListItem(
    listItem: ListItem,
    bullet: String,
    vaultRoot: File,
    markdown: String,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    val context = LocalContext.current
    val itemText = extractPlainText(listItem).trim()

    // Detect task checklist: e.g. [ ] or [x]
    val isChecklist = itemText.startsWith("[ ] ") || itemText.startsWith("[x] ") || itemText.startsWith("[X] ")
    if (isChecklist) {
        val isChecked = itemText.startsWith("[x] ") || itemText.startsWith("[X] ")
        val cleanTaskText = itemText.removeRange(0, 4)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { newChecked ->
                    val lines = markdown.lines()
                    val targetLineIndex = lines.indexOfFirst { it.contains(itemText) }
                    if (targetLineIndex != -1) {
                        onToggleTask?.invoke(targetLineIndex, newChecked)
                    }
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(28.dp).padding(end = 4.dp)
            )
            Text(
                text = cleanTaskText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
            )
        }
        return
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = bullet,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            var child: Node? = listItem.firstChild
            while (child != null) {
                val inline = buildInlineAnnotatedString(child, onLinkClick != null || onWikiLinkClick != null)
                ClickableText(
                    text = inline,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    onClick = { offset -> handleLinkClick(offset, inline, context, onLinkClick, onWikiLinkClick) }
                )
                child = child.next
            }
        }
    }
}

@Composable
private fun RenderTable(
    tableBlock: TableBlock,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            var section: Node? = tableBlock.firstChild
            while (section != null) {
                when (section) {
                    is TableHead -> {
                        var row: Node? = section.firstChild
                        while (row != null) {
                            if (row is TableRow) {
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(8.dp)
                                ) {
                                    var cell: Node? = row.firstChild
                                    while (cell != null) {
                                        if (cell is TableCell) {
                                            val text = buildInlineAnnotatedString(cell, onLinkClick != null || onWikiLinkClick != null)
                                            ClickableText(
                                                text = text,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                onClick = { offset -> handleLinkClick(offset, text, context, onLinkClick, onWikiLinkClick) }
                                            )
                                        }
                                        cell = cell.next
                                    }
                                }
                            }
                            row = row.next
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    is TableBody -> {
                        var row: Node? = section.firstChild
                        while (row != null) {
                            if (row is TableRow) {
                                Row(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    var cell: Node? = row.firstChild
                                    while (cell != null) {
                                        if (cell is TableCell) {
                                            val text = buildInlineAnnotatedString(cell, onLinkClick != null || onWikiLinkClick != null)
                                            ClickableText(
                                                text = text,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onBackground
                                                ),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                onClick = { offset -> handleLinkClick(offset, text, context, onLinkClick, onWikiLinkClick) }
                                            )
                                        }
                                        cell = cell.next
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                            row = row.next
                        }
                    }
                }
                section = section.next
            }
        }
    }
}

@Composable
private fun RenderImage(image: Image, vaultRoot: File) {
    val context = LocalContext.current
    val url = image.destination

    val imageFile = if (url.startsWith("http://") || url.startsWith("https://")) {
        null
    } else {
        File(vaultRoot, url.removePrefix("/"))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageFile ?: url)
                .crossfade(true)
                .build(),
            contentDescription = image.title ?: "Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

private fun buildInlineAnnotatedString(node: Node, enableLinks: Boolean): AnnotatedString {
    return buildAnnotatedString {
        appendNode(node, this, enableLinks)
    }
}

private fun appendNode(node: Node, builder: AnnotatedString.Builder, enableLinks: Boolean) {
    var child: Node? = node.firstChild
    while (child != null) {
        when (child) {
            is org.commonmark.node.Text -> {
                val literal = child.literal
                val links = com.nool.app.data.index.WikiLinkParser.parseLinks(literal)
                if (links.isEmpty()) {
                    builder.append(literal)
                } else {
                    var currentIndex = 0
                    for (link in links) {
                        if (link.startIndex > currentIndex) {
                            builder.append(literal.substring(currentIndex, link.startIndex))
                        }
                        if (link.isEmbed) {
                            builder.pushStyle(
                                SpanStyle(
                                    color = Color(0xFF6B8AFD),
                                    background = Color(0x226B8AFD),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            builder.append(" ![[${link.target}]] ")
                            builder.pop()
                        } else {
                            val displayText = link.alias ?: (link.target + (if (link.heading != null) "#${link.heading}" else ""))
                            builder.pushStringAnnotation(tag = "WIKILINK", annotation = link.target)
                            builder.pushStyle(
                                SpanStyle(
                                    color = Color(0xFF4C6EF5),
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            builder.append(displayText)
                            builder.pop()
                            builder.pop()
                        }
                        currentIndex = link.endIndex
                    }
                    if (currentIndex < literal.length) {
                        builder.append(literal.substring(currentIndex))
                    }
                }
            }
            is StrongEmphasis -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendNode(child, builder, enableLinks)
                builder.pop()
            }
            is Emphasis -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendNode(child, builder, enableLinks)
                builder.pop()
            }
            is Strikethrough -> {
                builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                appendNode(child, builder, enableLinks)
                builder.pop()
            }
            is Code -> {
                builder.pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x22888888),
                        fontSize = 13.sp
                    )
                )
                builder.append(" ${child.literal} ")
                builder.pop()
            }
            is Link -> {
                val url = child.destination
                builder.pushStringAnnotation(tag = "URL", annotation = url)
                builder.pushStyle(
                    SpanStyle(
                        color = Color(0xFF6B8AFD),
                        textDecoration = TextDecoration.Underline
                    )
                )
                appendNode(child, builder, enableLinks)
                builder.pop()
                builder.pop()
            }
            is SoftLineBreak -> {
                builder.append(" ")
            }
            is HardLineBreak -> {
                builder.append("\n")
            }
            else -> {
                appendNode(child, builder, enableLinks)
            }
        }
        child = child.next
    }
}

private fun handleLinkClick(
    offset: Int,
    annotatedString: AnnotatedString,
    context: Context,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)? = null
) {
    val wikiAnnotations = annotatedString.getStringAnnotations(tag = "WIKILINK", start = offset, end = offset)
    wikiAnnotations.firstOrNull()?.let { annotation ->
        onWikiLinkClick?.invoke(annotation.item)
        return
    }

    val annotations = annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
    annotations.firstOrNull()?.let { annotation ->
        val url = annotation.item
        if (onLinkClick != null) {
            onLinkClick(url)
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }
}

private fun extractPlainText(node: Node): String {
    val sb = StringBuilder()
    fun traverse(n: Node) {
        if (n is org.commonmark.node.Text) sb.append(n.literal)
        var c = n.firstChild
        while (c != null) {
            traverse(c)
            c = c.next
        }
    }
    traverse(node)
    return sb.toString()
}
