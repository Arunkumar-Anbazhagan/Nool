package com.nool.app.ui.components.expressive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class SegmentedItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null
)

/**
 * Expressive Segmented Button Row:
 * A pill-shaped segmented selector using Material 3's [SingleChoiceSegmentedButtonRow].
 * Ideal for switching modes (e.g. System | Light | Dark).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedButtonRow(
    items: List<SegmentedItem<T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = item.value == selectedItem
            SegmentedButton(
                selected = isSelected,
                onClick = { onItemSelected(item.value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = isSelected) {
                        if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
