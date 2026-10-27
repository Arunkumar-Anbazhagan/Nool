package com.nool.app.ui.graph

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.nool.app.domain.model.GraphData
import com.nool.app.domain.model.GraphNode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun GraphCanvas(
    data: GraphData,
    selectedNodeId: String?,
    highlightQuery: String = "",
    onNodeClick: (GraphNode) -> Unit,
    onNodeLongClick: (GraphNode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.outlineVariant
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    val nodeMap = remember(data.nodes) { data.nodes.associateBy { it.id } }

    val textPaint = remember(onSurfaceColor) {
        Paint().apply {
            color = onSurfaceColor.toArgb()
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.2f, 4.0f)
                    offset += pan
                }
            }
            .pointerInput(data.nodes, scale, offset) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        // Transform tap coords to graph world coords
                        val worldX = (tapOffset.x - centerX - offset.x) / scale
                        val worldY = (tapOffset.y - centerY - offset.y) / scale

                        // Hit test nodes (closest within radius + margin)
                        val hitNode = data.nodes.minByOrNull { node ->
                            val dx = node.x - worldX
                            val dy = node.y - worldY
                            sqrt(dx * dx + dy * dy)
                        }

                        if (hitNode != null) {
                            val radius = (12f + min(hitNode.degree * 2.5f, 24f))
                            val dist = sqrt((hitNode.x - worldX) * (hitNode.x - worldX) + (hitNode.y - worldY) * (hitNode.y - worldY))
                            if (dist <= radius + 16f / scale) {
                                onNodeClick(hitNode)
                            }
                        }
                    },
                    onLongPress = { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val worldX = (tapOffset.x - centerX - offset.x) / scale
                        val worldY = (tapOffset.y - centerY - offset.y) / scale

                        val hitNode = data.nodes.minByOrNull { node ->
                            val dx = node.x - worldX
                            val dy = node.y - worldY
                            sqrt(dx * dx + dy * dy)
                        }

                        if (hitNode != null) {
                            val radius = (12f + min(hitNode.degree * 2.5f, 24f))
                            val dist = sqrt((hitNode.x - worldX) * (hitNode.x - worldX) + (hitNode.y - worldY) * (hitNode.y - worldY))
                            if (dist <= radius + 16f / scale) {
                                onNodeLongClick(hitNode)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            // 1. Draw Edges
            for (edge in data.edges) {
                val sNode = nodeMap[edge.source] ?: continue
                val tNode = nodeMap[edge.target] ?: continue

                val startX = centerX + offset.x + sNode.x * scale
                val startY = centerY + offset.y + sNode.y * scale
                val endX = centerX + offset.x + tNode.x * scale
                val endY = centerY + offset.y + tNode.y * scale

                val edgeColor = if (edge.source == selectedNodeId || edge.target == selectedNodeId) {
                    primaryColor.copy(alpha = 0.8f)
                } else {
                    secondaryColor.copy(alpha = 0.45f)
                }

                drawLine(
                    color = edgeColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (edge.source == selectedNodeId || edge.target == selectedNodeId) 3f * scale else 1.5f * scale
                )
            }

            // 2. Draw Nodes
            for (node in data.nodes) {
                val screenX = centerX + offset.x + node.x * scale
                val screenY = centerY + offset.y + node.y * scale

                // Culling: don't draw if outside viewport
                if (screenX < -100 || screenX > size.width + 100 || screenY < -100 || screenY > size.height + 100) {
                    continue
                }

                val baseRadius = (10f + min(node.degree * 2.2f, 22f))
                val radius = baseRadius * scale

                val isHighlighted = highlightQuery.isNotBlank() && (
                    node.label.contains(highlightQuery, ignoreCase = true) ||
                    node.tags.any { it.contains(highlightQuery, ignoreCase = true) }
                )
                val isSelected = node.id == selectedNodeId

                val nodeColor = when {
                    isSelected -> primaryColor
                    isHighlighted -> tertiaryColor
                    node.isFavorite -> Color(0xFFE5A93C) // Warm gold
                    node.isPinned -> Color(0xFF5B92E5) // Soft blue
                    node.noteType.equals("Task", ignoreCase = true) -> Color(0xFF4CAF50)
                    node.noteType.equals("Project", ignoreCase = true) -> Color(0xFF9C27B0)
                    node.noteType.equals("Person", ignoreCase = true) -> Color(0xFFFF9800)
                    else -> primaryColor.copy(alpha = 0.85f)
                }

                // Node circle
                drawCircle(
                    color = nodeColor,
                    radius = radius,
                    center = Offset(screenX, screenY)
                )

                // White/Surface ring for selected node
                if (isSelected) {
                    drawCircle(
                        color = surfaceColor,
                        radius = radius + 3f * scale,
                        center = Offset(screenX, screenY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * scale)
                    )
                }

                // 3. Level-of-Detail Labels
                // Render label if:
                // - Zoom level is reasonably high (scale >= 0.7)
                // - OR node is selected / highlighted
                // - OR node has high degree (degree >= 3)
                val showLabel = isSelected || isHighlighted || (scale >= 0.75f) || (node.degree >= 3 && scale >= 0.45f)

                if (showLabel) {
                    textPaint.textSize = (13f * scale).coerceIn(16f, 32f)
                    val textX = screenX + radius + 6f * scale
                    val textY = screenY + (textPaint.textSize / 3f)
                    drawContext.canvas.nativeCanvas.drawText(
                        node.label,
                        textX,
                        textY,
                        textPaint
                    )
                }
            }
        }
    }
}
