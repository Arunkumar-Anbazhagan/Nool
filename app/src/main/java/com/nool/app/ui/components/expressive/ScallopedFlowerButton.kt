package com.nool.app.ui.components.expressive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Expressive Scalloped Flower Button:
 * An expressive FAB with an 8-to-12 lobed wavy flower outline.
 * Ideal for flanking the floating pill dock or as a standalone primary action button.
 */
@Composable
fun ScallopedFlowerButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lobes: Int = 12,
    scallopDepth: Float = 0.10f,
    size: Dp = 64.dp,
    iconSize: Dp = 26.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val flowerShape = ScallopedFlowerShape(lobes = lobes, scallopDepth = scallopDepth)

    Surface(
        modifier = modifier
            .size(size)
            .shadow(elevation = 6.dp, shape = flowerShape)
            .clip(flowerShape)
            .clickable(onClick = onClick),
        shape = flowerShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
