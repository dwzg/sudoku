package de.wtzg.sudoku.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Digits 1-9, laid out as one row (portrait) or a 3x3 grid (landscape). */
@Composable
fun NumberPad(
    remaining: (Int) -> Int,
    notesMode: Boolean,
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 9,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..9).chunked(columns).forEach { rowDigits ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowDigits.forEach { d ->
                    NumberKey(
                        digit = d,
                        remaining = remaining(d),
                        notesMode = notesMode,
                        enabled = enabled,
                        onClick = { onDigit(d) },
                        modifier = Modifier.weight(1f),
                        tall = columns == 9,
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberKey(
    digit: Int,
    remaining: Int,
    notesMode: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    tall: Boolean,
) {
    val done = remaining <= 0
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled && !done,
        shape = RoundedCornerShape(14.dp),
        color = if (notesMode) colors.secondaryContainer else colors.surfaceContainerHigh,
        modifier = modifier.then(if (tall) Modifier.height(64.dp) else Modifier.aspectRatio(1.4f)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(if (done) 0.25f else 1f),
        ) {
            Text(
                text = digit.toString(),
                fontSize = if (notesMode) 20.sp else 26.sp,
                fontWeight = if (notesMode) FontWeight.Medium else FontWeight.SemiBold,
                color = if (notesMode) colors.onSecondaryContainer else colors.primary,
            )
            if (!done) {
                Text(
                    text = remaining.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    badge: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (active) colors.primaryContainer else colors.surfaceContainerHigh, label = "tool"
    )
    val content = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant
    Column(
        modifier.alpha(if (enabled) 1f else 0.4f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BadgedBox(
            badge = {
                if (badge != null) {
                    Badge(
                        containerColor = if (active) colors.primary else colors.outline,
                        contentColor = if (active) colors.onPrimary else colors.surface,
                    ) { Text(badge) }
                }
            }
        ) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                shape = CircleShape,
                color = container,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, tint = content)
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
