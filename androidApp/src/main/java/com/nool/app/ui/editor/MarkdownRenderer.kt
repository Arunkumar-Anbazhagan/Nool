package com.nool.app.ui.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nool.app.ui.editor.callout.CalloutHelper
import com.nool.app.ui.editor.callout.CalloutInfo
import com.nool.app.ui.editor.embed.NoteEmbedHelper
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.nool.app.ui.theme.Dimens
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
    currentNotePath: String = "",
    recursionDepth: Int = 0,
    visitedPaths: Set<String> = if (currentNotePath.isNotEmpty()) setOf(currentNotePath) else emptySet(),
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
                recursionDepth = recursionDepth,
                visitedPaths = visitedPaths,
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
    recursionDepth: Int,
    visitedPaths: Set<String>,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    when (node) {
        is Heading -> {
            RenderHeading(node, onLinkClick, onWikiLinkClick)
        }
        is Paragraph -> {
            RenderParagraph(node, vaultRoot, markdown, recursionDepth, visitedPaths, onToggleTask, onLinkClick, onWikiLinkClick)
        }
        is BlockQuote -> {
            RenderBlockQuote(node, vaultRoot, markdown, recursionDepth, visitedPaths, onToggleTask, onLinkClick, onWikiLinkClick)
        }
        is FencedCodeBlock -> {
            val info = node.info?.trim()?.lowercase()
            if (info == "mermaid") {
                com.nool.app.ui.editor.mermaid.MermaidDiagramCard(
                    code = node.literal,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (info == "math" || info == "latex") {
                com.nool.app.ui.editor.math.MathBlockCard(
                    formula = node.literal,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                RenderCodeBlock(node.literal, node.info)
            }
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
    markdown: String,
    recursionDepth: Int,
    visitedPaths: Set<String>,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    val plain = extractPlainText(paragraph).trim()
    val embedMatch = Regex("""^!\[\[([^\]\n]+)\]\]$""").matchEntire(plain)
    if (embedMatch != null) {
        val target = embedMatch.groupValues[1].trim()
        val isAudio = target.endsWith(".m4a", true) || target.endsWith(".mp3", true) ||
                      target.endsWith(".wav", true) || target.endsWith(".aac", true) ||
                      target.endsWith(".ogg", true)

        if (isAudio) {
            val file = if (target.startsWith("attachments/")) File(vaultRoot, target) else File(vaultRoot, "attachments/$target")
            com.nool.app.ui.editor.audio.AudioPlayerCard(
                audioFile = file,
                fileName = target.substringAfterLast("/")
            )
            return
        }

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
            RenderNoteEmbed(
                target = target,
                vaultRoot = vaultRoot,
                recursionDepth = recursionDepth,
                visitedPaths = visitedPaths,
                onToggleTask = onToggleTask,
                onLinkClick = onLinkClick,
                onWikiLinkClick = onWikiLinkClick
            )
            return
        }
    }

    // Check if the paragraph contains an image as its primary child
    val firstChild = paragraph.firstChild
    if (firstChild is Image) {
        RenderImage(firstChild, vaultRoot)
        return
    }

    if (firstChild is org.commonmark.node.Text && paragraph.lastChild == firstChild) {
        val rawText = firstChild.literal.trim()
        if (rawText.startsWith("$$") && rawText.endsWith("$$") && rawText.length >= 4) {
            com.nool.app.ui.editor.math.MathBlockCard(
                formula = rawText,
                modifier = Modifier.fillMaxWidth()
            )
            return
        }
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
private fun RenderNoteEmbed(
    target: String,
    vaultRoot: File,
    recursionDepth: Int,
    visitedPaths: Set<String>,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    val noteTarget = target.substringBefore("#").substringBefore("|").trim()
    val headingTarget = if (target.contains("#")) target.substringAfter("#").substringBefore("|").trim() else null

    // 1. Cycle / Depth limit check
    if (recursionDepth >= NoteEmbedHelper.MAX_RECURSION_DEPTH) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Embed depth limit reached for '$noteTarget'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        return
    }

    val noteFile = remember(target, vaultRoot) {
        NoteEmbedHelper.resolveNoteFile(vaultRoot, noteTarget)
    }

    // 2. Note not found
    if (noteFile == null || !noteFile.exists()) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (headingTarget != null) "$noteTarget #$headingTarget" else noteTarget,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Embedded note does not exist • Tap to create",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        return
    }

    // 3. Cycle detection
    val canonicalPath = noteFile.relativeTo(vaultRoot).path.replace('\\', '/')
    if (visitedPaths.contains(canonicalPath)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cycle detected in note embed: '$canonicalPath'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        return
    }

    // 4. Extract content and render inline
    val rawContent = remember(noteFile) {
        try { noteFile.readText() } catch (e: Exception) { "" }
    }

    val embedMarkdown = remember(rawContent, headingTarget) {
        if (headingTarget.isNullOrBlank()) {
            rawContent
        } else {
            NoteEmbedHelper.extractHeadingSection(rawContent, headingTarget)
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Embed Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onWikiLinkClick?.invoke(noteTarget) }
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (headingTarget != null) "$noteTarget > $headingTarget" else noteTarget,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open embedded note",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Inline markdown rendering of embedded note content
            MarkdownRenderer(
                markdown = embedMarkdown,
                vaultRoot = vaultRoot,
                modifier = Modifier.fillMaxWidth(),
                currentNotePath = canonicalPath,
                recursionDepth = recursionDepth + 1,
                visitedPaths = visitedPaths + canonicalPath,
                onToggleTask = onToggleTask,
                onLinkClick = onLinkClick,
                onWikiLinkClick = onWikiLinkClick
            )
        }
    }
}

@Composable
private fun RenderBlockQuote(
    blockQuote: BlockQuote,
    vaultRoot: File,
    markdown: String,
    recursionDepth: Int,
    visitedPaths: Set<String>,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    // Check if this blockquote is an Obsidian callout
    val firstBlock = blockQuote.firstChild
    var calloutInfo: CalloutInfo? = null
    var firstParagraphRestText: String? = null

    if (firstBlock is Paragraph) {
        val firstInline = firstBlock.firstChild
        if (firstInline is org.commonmark.node.Text) {
            val literal = firstInline.literal
            val lines = literal.lines()
            val firstLine = lines.firstOrNull().orEmpty()
            val parsed = CalloutHelper.parseCalloutHeader(firstLine)
            if (parsed != null) {
                calloutInfo = parsed
                val restLines = lines.drop(1).joinToString("\n")
                if (restLines.isNotBlank()) {
                    firstParagraphRestText = restLines
                }
            }
        }
    }

    if (calloutInfo != null && firstBlock is Paragraph) {
        RenderCallout(
            callout = calloutInfo,
            firstParagraph = firstBlock,
            firstParagraphRemainingText = firstParagraphRestText,
            vaultRoot = vaultRoot,
            markdown = markdown,
            recursionDepth = recursionDepth,
            visitedPaths = visitedPaths,
            onToggleTask = onToggleTask,
            onLinkClick = onLinkClick,
            onWikiLinkClick = onWikiLinkClick
        )
        return
    }

    // Default Blockquote Rendering
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
                RenderBlockNode(child, vaultRoot, markdown, recursionDepth, visitedPaths, onToggleTask, onLinkClick, onWikiLinkClick)
                child = child.next
            }
        }
    }
}

@Composable
private fun RenderCallout(
    callout: CalloutInfo,
    firstParagraph: Paragraph,
    firstParagraphRemainingText: String?,
    vaultRoot: File,
    markdown: String,
    recursionDepth: Int,
    visitedPaths: Set<String>,
    onToggleTask: ((lineIndex: Int, isChecked: Boolean) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    var isExpanded by remember { mutableStateOf(!callout.isInitiallyCollapsed) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = callout.color.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, callout.color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .background(callout.color)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Header (Clickable if collapsible)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (callout.isCollapsible) {
                                Modifier.clickable { isExpanded = !isExpanded }
                            } else Modifier
                        )
                ) {
                    Icon(
                        imageVector = callout.icon,
                        contentDescription = null,
                        tint = callout.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = callout.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = callout.color,
                        modifier = Modifier.weight(1f)
                    )

                    if (callout.isCollapsible) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = callout.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Body content
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!firstParagraphRemainingText.isNullOrBlank()) {
                            Text(
                                text = firstParagraphRemainingText,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Render subsequent block nodes in the callout
                        var nextBlock = firstParagraph.next
                        while (nextBlock != null) {
                            RenderBlockNode(nextBlock, vaultRoot, markdown, recursionDepth, visitedPaths, onToggleTask, onLinkClick, onWikiLinkClick)
                            nextBlock = nextBlock.next
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderCodeBlock(code: String, language: String?) {
    val clipboard = LocalClipboardManager.current
    val keywordColor = MaterialTheme.colorScheme.primary
    val stringColor = MaterialTheme.colorScheme.tertiary
    val numberColor = MaterialTheme.colorScheme.secondary
    val commentColor = MaterialTheme.colorScheme.outline
    val typeColor = MaterialTheme.colorScheme.primary
    val plainColor = MaterialTheme.colorScheme.onSurfaceVariant

    val annotatedString = remember(code, language, keywordColor, stringColor, numberColor, commentColor) {
        val highlighted = com.nool.core.markdown.SyntaxHighlighter.highlight(code.trimEnd(), language)
        buildAnnotatedString {
            for (token in highlighted) {
                when (token.type) {
                    com.nool.core.markdown.SyntaxTokenType.KEYWORD -> withStyle(androidx.compose.ui.text.SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) { append(token.text) }
                    com.nool.core.markdown.SyntaxTokenType.STRING -> withStyle(androidx.compose.ui.text.SpanStyle(color = stringColor)) { append(token.text) }
                    com.nool.core.markdown.SyntaxTokenType.NUMBER -> withStyle(androidx.compose.ui.text.SpanStyle(color = numberColor)) { append(token.text) }
                    com.nool.core.markdown.SyntaxTokenType.COMMENT -> withStyle(androidx.compose.ui.text.SpanStyle(color = commentColor, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) { append(token.text) }
                    com.nool.core.markdown.SyntaxTokenType.TYPE_OR_CLASS -> withStyle(androidx.compose.ui.text.SpanStyle(color = typeColor, fontWeight = FontWeight.SemiBold)) { append(token.text) }
                    com.nool.core.markdown.SyntaxTokenType.FUNCTION -> withStyle(androidx.compose.ui.text.SpanStyle(color = plainColor, fontWeight = FontWeight.Medium)) { append(token.text) }
                    com.nool.core.markdown.SyntaxTokenType.PLAIN -> withStyle(androidx.compose.ui.text.SpanStyle(color = plainColor)) { append(token.text) }
                }
            }
        }
    }

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
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
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
                    val targetLineIndex = lines.indexOfFirst { line ->
                        val trimmed = line.trim()
                        trimmed.startsWith("- [ ] $cleanTaskText") ||
                        trimmed.startsWith("- [x] $cleanTaskText") ||
                        trimmed.startsWith("- [X] $cleanTaskText") ||
                        trimmed.startsWith("* [ ] $cleanTaskText") ||
                        trimmed.startsWith("* [x] $cleanTaskText") ||
                        trimmed.startsWith("* [X] $cleanTaskText") ||
                        (trimmed.contains(cleanTaskText) && (trimmed.contains("[ ]") || trimmed.contains("[x]") || trimmed.contains("[X]")))
                    }
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

private data class TableCellModel(
    val content: AnnotatedString,
    val alignment: TableCell.Alignment?,
    val isHeader: Boolean
)

private data class TableRowModel(
    val isHeader: Boolean,
    val cells: List<TableCellModel>
)

@Composable
private fun RenderTable(
    tableBlock: TableBlock,
    onLinkClick: ((url: String) -> Unit)?,
    onWikiLinkClick: ((target: String) -> Unit)?
) {
    val context = LocalContext.current

    val rows = remember(tableBlock) {
        val extractedRows = mutableListOf<TableRowModel>()
        var section: Node? = tableBlock.firstChild
        while (section != null) {
            val isHead = section is TableHead
            var rowNode: Node? = section.firstChild
            while (rowNode != null) {
                if (rowNode is TableRow) {
                    val cells = mutableListOf<TableCellModel>()
                    var cellNode: Node? = rowNode.firstChild
                    while (cellNode != null) {
                        if (cellNode is TableCell) {
                            val text = buildInlineAnnotatedString(cellNode, onLinkClick != null || onWikiLinkClick != null)
                            cells.add(
                                TableCellModel(
                                    content = text,
                                    alignment = cellNode.alignment,
                                    isHeader = isHead || cellNode.isHeader
                                )
                            )
                        }
                        cellNode = cellNode.next
                    }
                    if (cells.isNotEmpty()) {
                        extractedRows.add(TableRowModel(isHeader = isHead, cells = cells))
                    }
                }
                rowNode = rowNode.next
            }
            section = section.next
        }
        extractedRows
    }

    if (rows.isEmpty()) return

    val numColumns = remember(rows) { rows.maxOf { it.cells.size } }
    if (numColumns == 0) return

    val columnWidths = remember(rows, numColumns) {
        (0 until numColumns).map { col ->
            var maxChars = 6
            for (r in rows) {
                if (col < r.cells.size) {
                    maxChars = maxOf(maxChars, r.cells[col].content.text.length)
                }
            }
            val estimated = (maxChars * 8.5f + 32).dp
            estimated.coerceIn(110.dp, 340.dp)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CornerSmall))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Dimens.CornerSmall))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            var bodyRowIndex = 0
            rows.forEachIndexed { index, row ->
                val isHeaderRow = row.isHeader
                val rowBg = when {
                    isHeaderRow -> MaterialTheme.colorScheme.surfaceContainerHigh
                    bodyRowIndex % 2 == 1 -> MaterialTheme.colorScheme.surfaceContainerLow
                    else -> MaterialTheme.colorScheme.surface
                }
                if (!isHeaderRow) {
                    bodyRowIndex++
                }

                Row(
                    modifier = Modifier
                        .background(rowBg)
                        .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (colIndex in 0 until numColumns) {
                        if (colIndex > 0) {
                            VerticalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = if (isHeaderRow) 0.8f else 0.4f
                                ),
                                thickness = 1.dp
                            )
                        }

                        val cell = row.cells.getOrNull(colIndex)
                        val alignment = cell?.alignment
                        val cellAlignment = when (alignment) {
                            TableCell.Alignment.CENTER -> Alignment.Center
                            TableCell.Alignment.RIGHT -> Alignment.CenterEnd
                            else -> Alignment.CenterStart
                        }
                        val textAlign = when (alignment) {
                            TableCell.Alignment.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                            TableCell.Alignment.RIGHT -> androidx.compose.ui.text.style.TextAlign.Right
                            else -> androidx.compose.ui.text.style.TextAlign.Left
                        }

                        Box(
                            modifier = Modifier
                                .width(columnWidths[colIndex])
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = cellAlignment
                        ) {
                            if (cell != null) {
                                val textStyle = if (isHeaderRow) {
                                    MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = textAlign
                                    )
                                } else {
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = textAlign
                                    )
                                }

                                ClickableText(
                                    text = cell.content,
                                    style = textStyle,
                                    onClick = { offset ->
                                        handleLinkClick(offset, cell.content, context, onLinkClick, onWikiLinkClick)
                                    }
                                )
                            }
                        }
                    }
                }

                if (index < rows.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = if (isHeaderRow) 1f else 0.5f
                        ),
                        thickness = if (isHeaderRow) 1.5.dp else 1.dp
                    )
                }
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
                val links = com.nool.core.markdown.WikiLinkParser.parseLinks(literal)
                if (links.isEmpty()) {
                    appendFormattedText(literal, builder)
                } else {
                    var currentIndex = 0
                    for (link in links) {
                        if (link.startIndex > currentIndex) {
                            appendFormattedText(literal.substring(currentIndex, link.startIndex), builder)
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
                        appendFormattedText(literal.substring(currentIndex), builder)
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

private fun appendFormattedText(text: String, builder: AnnotatedString.Builder) {
    if (!text.contains("$") && !text.contains("[^")) {
        builder.append(text)
        return
    }

    var index = 0
    val tokenRegex = Regex("""(?<!\$)\$(?!\$)([^\$\n]+?)\$(?!\$)|\[\^([a-zA-Z0-9_-]+)\](?!:)""")
    val matches = tokenRegex.findAll(text)

    for (match in matches) {
        if (match.range.first > index) {
            builder.append(text.substring(index, match.range.first))
        }

        val mathGroup = match.groups[1]
        val footnoteGroup = match.groups[2]

        if (mathGroup != null) {
            builder.pushStyle(
                SpanStyle(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    background = Color(0x18BA68C8),
                    color = Color(0xFF9C27B0),
                    fontSize = 14.sp
                )
            )
            builder.append(" ${mathGroup.value} ")
            builder.pop()
        } else if (footnoteGroup != null) {
            val footnoteId = footnoteGroup.value
            builder.pushStringAnnotation(tag = "FOOTNOTE", annotation = footnoteId)
            builder.pushStyle(
                SpanStyle(
                    color = Color(0xFF6750A4),
                    fontSize = 11.sp,
                    baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript,
                    fontWeight = FontWeight.Bold
                )
            )
            builder.append("[$footnoteId]")
            builder.pop()
            builder.pop()
        }

        index = match.range.last + 1
    }

    if (index < text.length) {
        builder.append(text.substring(index))
    }
}
