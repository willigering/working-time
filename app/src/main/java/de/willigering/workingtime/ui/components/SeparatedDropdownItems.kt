package de.willigering.workingtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.willigering.workingtime.ui.theme.AppColors

private val RowEven = Color(0xFF181818)
private val RowOdd = Color(0xFF2C2C2C)
private val SelectedRow = Color(0x40FFB300)
private val DividerColor = Color(0x52FFFFFF)

/**
 * Dropdown rows with zebra stripes, a visible hairline, and a selected state
 * so consecutive names stay easy to tell apart.
 */
@Composable
fun <T> SeparatedDropdownItems(
    items: List<T>,
    selected: T? = null,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    leadingIcon: ImageVector = Icons.Rounded.Business,
    isSame: (T, T) -> Boolean = { a, b -> a == b },
) {
    items.forEachIndexed { index, item ->
        val chosen = selected != null && isSame(item, selected)
        val rowColor = when {
            chosen -> SelectedRow
            index % 2 == 0 -> RowEven
            else -> RowOdd
        }
        if (index > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(DividerColor),
            )
        }
        DropdownMenuItem(
            text = {
                Text(
                    label(item),
                    color = AppColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            leadingIcon = {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = if (chosen) AppColors.Accent else AppColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = if (chosen) {
                {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                null
            },
            onClick = { onSelect(item) },
            modifier = Modifier.background(rowColor),
            colors = MenuDefaults.itemColors(
                textColor = AppColors.TextPrimary,
                leadingIconColor = AppColors.TextSecondary,
                trailingIconColor = AppColors.Accent,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}
