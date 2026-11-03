package com.nool.app.ui.components.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavDockItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

/**
 * Expressive Floating Pill Navigation Dock:
 * A detached floating capsule bottom dock that hovers gracefully above system navigation insets.
 * Features:
 * - Rounded capsule shape (32dp)
 * - Elevated surface with subtle border / shadow
 * - Pill-shaped active tab indicator with spring animations
 * - Optional leading or trailing slot (e.g., for flanking scalloped action buttons)
 */
@Composable
fun FloatingPillNavDock(
    items: List<NavDockItem>,
    selectedItemId: String,
    onItemSelected: (NavDockItem) -> Unit,
    modifier: Modifier = Modifier,
    leadingActionButton: (@Composable () -> Unit)? = null,
    trailingActionButton: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    activePillColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    activeContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    inactiveContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val navInsets = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = navInsets.calculateBottomPadding() + 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Optional flanking leading action button
            leadingActionButton?.invoke()

            // Main floating capsule dock
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = containerColor,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.height(64.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEach { item ->
                        val isSelected = item.id == selectedItemId
                        val animatedBgColor by animateColorAsState(
                            targetValue = if (isSelected) activePillColor else Color.Transparent,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "nav_pill_bg"
                        )
                        val animatedContentColor by animateColorAsState(
                            targetValue = if (isSelected) activeContentColor else inactiveContentColor,
                            label = "nav_pill_content"
                        )

                        val interactionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(animatedBgColor)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onItemSelected(item) }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                                    contentDescription = item.label,
                                    tint = animatedContentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (isSelected) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = animatedContentColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Optional flanking trailing action button
            trailingActionButton?.invoke()
        }
    }
}
