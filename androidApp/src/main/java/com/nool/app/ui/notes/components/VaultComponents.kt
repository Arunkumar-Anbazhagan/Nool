package com.nool.app.ui.notes.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nool.app.ui.notes.Breadcrumb
import com.nool.app.ui.theme.Dimens
import com.nool.core.model.VaultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BreadcrumbsBar(
    breadcrumbs: List<Breadcrumb>,
    onCrumbClick: (Breadcrumb) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = Dimens.ScreenHorizontalMargin, vertical = Dimens.Spacing8)
    ) {
        breadcrumbs.forEachIndexed { index, crumb ->
            val isLast = index == breadcrumbs.lastIndex

            Surface(
                shape = RoundedCornerShape(Dimens.CornerSmall),
                color = if (isLast) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.CornerSmall))
                    .clickable { onCrumbClick(crumb) }
            ) {
                Text(
                    text = crumb.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal),
                    color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimens.Spacing12, vertical = Dimens.Spacing4)
                )
            }

            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = Dimens.Spacing4)
                        .size(10.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun VaultItemRow(
    item: VaultItem,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onToggleArchive: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    VaultItemCard(
        item = item,
        isGrid = false,
        onClick = onClick,
        onRename = onRename,
        onMove = onMove,
        onDelete = onDelete,
        onTogglePin = onTogglePin,
        onToggleFavorite = onToggleFavorite,
        onToggleArchive = onToggleArchive,
        modifier = modifier
    )
}

@Composable
fun VaultItemCard(
    item: VaultItem,
    isGrid: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onToggleArchive: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationLow),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        if (isGrid) {
            // Grid Layout Card (Uniform card layout for 2+ column adaptive grids)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.CardContentPadding)
            ) {
                // Top header: Icon + Badges + Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(Dimens.CornerSmall),
                        color = if (item.isDirectory) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.isDirectory) Icons.Filled.Folder else Icons.Outlined.Description,
                                contentDescription = null,
                                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(Dimens.IconSmall)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(Dimens.Spacing8))

                    // Status indicators
                    if (item is VaultItem.NoteItem) {
                        if (item.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(Dimens.Spacing4))
                        }
                        if (item.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(Dimens.Spacing4))
                        }
                        if (item.isArchived) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.CornerXSmall),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "Archived",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = Dimens.Spacing4, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        ItemDropdownMenu(
                            expanded = menuExpanded,
                            item = item,
                            onDismiss = { menuExpanded = false },
                            onTogglePin = onTogglePin,
                            onToggleFavorite = onToggleFavorite,
                            onToggleArchive = onToggleArchive,
                            onRename = onRename,
                            onMove = onMove,
                            onDelete = onDelete
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.Spacing8))

                // Title limited to 1-2 lines with ellipsis
                Text(
                    text = if (item is VaultItem.NoteItem) item.title else item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item is VaultItem.NoteItem) {
                    if (item.excerpt.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Dimens.Spacing4))
                        // Preview limited to 2-3 lines
                        Text(
                            text = item.excerpt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.Spacing8))

                    // Footer Row: Tags + Date
                    if (item.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            item.tags.take(3).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(Dimens.CornerXSmall),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = Dimens.Spacing4, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(Dimens.Spacing4))
                    }

                    Text(
                        text = formatTimestamp(item.lastModified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else if (item is VaultItem.FolderItem) {
                    Spacer(modifier = Modifier.height(Dimens.Spacing4))
                    Text(
                        text = "${item.childCount} ${if (item.childCount == 1) "item" else "items"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // List Layout Card (Uniform row with vertically centered elements)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.CardContentPadding)
            ) {
                // Leading Icon Box
                Surface(
                    shape = RoundedCornerShape(Dimens.CornerSmall),
                    color = if (item.isDirectory) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (item.isDirectory) Icons.Filled.Folder else Icons.Outlined.Description,
                            contentDescription = null,
                            tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(Dimens.IconMedium)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Dimens.Spacing12))

                // Content Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (item is VaultItem.NoteItem) item.title else item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item is VaultItem.NoteItem) {
                            if (item.isPinned) {
                                Spacer(modifier = Modifier.width(Dimens.Spacing4))
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            if (item.isFavorite) {
                                Spacer(modifier = Modifier.width(Dimens.Spacing4))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Favorite",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            if (item.isArchived) {
                                Spacer(modifier = Modifier.width(Dimens.Spacing4))
                                Surface(
                                    shape = RoundedCornerShape(Dimens.CornerXSmall),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "Archived",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(horizontal = Dimens.Spacing4, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (item is VaultItem.NoteItem) {
                        if (item.excerpt.isNotBlank()) {
                            Spacer(modifier = Modifier.height(Dimens.Spacing4))
                            Text(
                                text = item.excerpt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(Dimens.Spacing4))

                        // Footer Row: Tags + Date + Size
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (item.tags.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    item.tags.take(3).forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(Dimens.CornerXSmall),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = Dimens.Spacing4, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = formatTimestamp(item.lastModified),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = formatFileSize(item.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else if (item is VaultItem.FolderItem) {
                        Spacer(modifier = Modifier.height(Dimens.Spacing4))
                        Text(
                            text = "${item.childCount} ${if (item.childCount == 1) "item" else "items"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Trailing Menu Button
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(Dimens.MinTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ItemDropdownMenu(
                        expanded = menuExpanded,
                        item = item,
                        onDismiss = { menuExpanded = false },
                        onTogglePin = onTogglePin,
                        onToggleFavorite = onToggleFavorite,
                        onToggleArchive = onToggleArchive,
                        onRename = onRename,
                        onMove = onMove,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemDropdownMenu(
    expanded: Boolean,
    item: VaultItem,
    onDismiss: () -> Unit,
    onTogglePin: (() -> Unit)?,
    onToggleFavorite: (() -> Unit)?,
    onToggleArchive: (() -> Unit)?,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (item is VaultItem.NoteItem) {
            DropdownMenuItem(
                text = { Text(if (item.isPinned) "Unpin Note" else "Pin Note") },
                leadingIcon = {
                    Icon(
                        if (item.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                        contentDescription = null
                    )
                },
                onClick = {
                    onDismiss()
                    onTogglePin?.invoke()
                }
            )
            DropdownMenuItem(
                text = { Text(if (item.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                leadingIcon = {
                    Icon(
                        if (item.isFavorite) Icons.Outlined.StarBorder else Icons.Filled.Star,
                        contentDescription = null
                    )
                },
                onClick = {
                    onDismiss()
                    onToggleFavorite?.invoke()
                }
            )
            DropdownMenuItem(
                text = { Text(if (item.isArchived) "Unarchive Note" else "Archive Note") },
                leadingIcon = {
                    Icon(
                        if (item.isArchived) Icons.Outlined.Archive else Icons.Filled.Archive,
                        contentDescription = null
                    )
                },
                onClick = {
                    onDismiss()
                    onToggleArchive?.invoke()
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = {
                onDismiss()
                onRename()
            }
        )
        DropdownMenuItem(
            text = { Text("Move") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
            onClick = {
                onDismiss()
                onMove()
            }
        )
        DropdownMenuItem(
            text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = {
                onDismiss()
                onDelete()
            }
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(date)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
