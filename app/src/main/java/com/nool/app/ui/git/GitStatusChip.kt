package com.nool.app.ui.git

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nool.app.sync.SyncStatus

/**
 * Expressive TopAppBar Git Sync Status Chip:
 * Displays real-time sync state with smooth animated transitions.
 */
@Composable
fun GitStatusChip(
    status: SyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val (icon, label, containerColor, contentColor, shouldRotate) = when (status) {
        is SyncStatus.Idle, is SyncStatus.Synced -> {
            Tuple5(Icons.Default.CloudDone, "Synced", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primary, false)
        }
        is SyncStatus.Syncing -> {
            Tuple5(Icons.Default.Sync, "Syncing", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, true)
        }
        is SyncStatus.Offline -> {
            Tuple5(Icons.Default.CloudOff, "Offline", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.outline, false)
        }
        is SyncStatus.Conflict -> {
            Tuple5(Icons.Default.Warning, "${status.conflictedFiles.size} Conflict", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, false)
        }
        is SyncStatus.AuthError -> {
            Tuple5(Icons.Default.Lock, "Auth", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, false)
        }
        is SyncStatus.Error -> {
            Tuple5(Icons.Default.ErrorOutline, "Error", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, false)
        }
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .size(16.dp)
                    .then(if (shouldRotate) Modifier.rotate(rotation) else Modifier)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
