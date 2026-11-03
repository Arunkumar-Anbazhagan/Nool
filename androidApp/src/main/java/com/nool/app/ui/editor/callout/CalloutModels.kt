package com.nool.app.ui.editor.callout

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CalloutInfo(
    val type: String,
    val title: String,
    val isCollapsible: Boolean,
    val isInitiallyCollapsed: Boolean,
    val color: Color,
    val icon: ImageVector
)

object CalloutHelper {
    fun parseCalloutHeader(firstLine: String): CalloutInfo? {
        val parsed = com.nool.core.markdown.CalloutParser.parseCalloutHeader(firstLine) ?: return null
        val (color, icon) = getCalloutStyle(parsed.type)

        return CalloutInfo(
            type = parsed.type,
            title = parsed.title,
            isCollapsible = parsed.isCollapsible,
            isInitiallyCollapsed = parsed.isInitiallyCollapsed,
            color = color,
            icon = icon
        )
    }

    fun getCalloutStyle(type: String): Pair<Color, ImageVector> {
        return when (type) {
            "note", "info", "todo" -> Pair(Color(0xFF2196F3), Icons.Default.Info)
            "tip", "hint", "important" -> Pair(Color(0xFF009688), Icons.Default.Lightbulb)
            "success", "check", "done" -> Pair(Color(0xFF4CAF50), Icons.Default.CheckCircle)
            "question", "help", "faq" -> Pair(Color(0xFFFFA000), Icons.Default.Help)
            "warning", "caution", "attention" -> Pair(Color(0xFFFF9800), Icons.Default.Warning)
            "failure", "fail", "missing", "danger", "error", "bug" -> Pair(Color(0xFFF44336), Icons.Default.Error)
            "example" -> Pair(Color(0xFF9C27B0), Icons.Default.List)
            "quote", "cite" -> Pair(Color(0xFF78909C), Icons.Default.FormatQuote)
            else -> Pair(Color(0xFF3F51B5), Icons.Default.Info)
        }
    }
}
