package app.auriel.basalt.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.auriel.basalt.core.dotmatrix.DotMatrixText
import app.auriel.basalt.core.dotmatrix.DotShape

/** An engraved section heading with a rule under it. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    val colors = LocalBasaltColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DotMatrixText(
            text = title,
            cellSize = 1.5f,
            shape = DotShape.Chunky,
            litColor = colors.bronze,
            unlitColor = Color.Transparent,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.bronzeDeep),
        )
    }
}

/** Label on the left, whatever control on the right. */
@Composable
fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
    control: @Composable () -> Unit,
) {
    val colors = LocalBasaltColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            DotMatrixText(
                text = label,
                cellSize = 1.6f,
                shape = DotShape.Chunky,
                litColor = colors.silver,
                unlitColor = Color.Transparent,
            )
            caption?.let {
                DotMatrixText(
                    text = it,
                    cellSize = 1.2f,
                    shape = DotShape.Chunky,
                    litColor = colors.pewter,
                    unlitColor = Color.Transparent,
                )
            }
        }
        control()
    }
}

/**
 * A numeric field with a decrement and an increment either side.
 *
 * Horizontal rather than the vertical [BasaltStepper], because in a settings
 * list the value has to sit on the row's baseline with its label.
 */
@Composable
fun InlineStepper(
    value: String,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val colors = LocalBasaltColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepChip(text = "-", onClick = onDown, clickLabel = label?.let { "Decrease $it" })
        DotMatrixText(
            text = value,
            cellSize = 1.9f,
            shape = DotShape.Chunky,
            litColor = colors.copper,
            unlitColor = Color.Transparent,
        )
        StepChip(text = "+", onClick = onUp, clickLabel = label?.let { "Increase $it" })
    }
}

@Composable
private fun StepChip(text: String, onClick: () -> Unit, clickLabel: String?) {
    val colors = LocalBasaltColors.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(colors.ironOxide)
            .clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        DotMatrixText(
            text = text,
            cellSize = 2.2f,
            shape = DotShape.Chunky,
            litColor = colors.copper,
            unlitColor = Color.Transparent,
        )
    }
}

/** The Monday-first day chips shared by alarms and bedtime. */
@Composable
fun WeekdayChips(
    isOn: (java.time.DayOfWeek) -> Boolean,
    onToggle: (java.time.DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBasaltColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayInitials.forEach { (day, initial) ->
            val on = isOn(day)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(if (on) colors.bronze else colors.ironOxide)
                    .clickable(role = Role.Checkbox, onClick = { onToggle(day) }),
                contentAlignment = Alignment.Center,
            ) {
                DotMatrixText(
                    text = initial,
                    cellSize = 1.8f,
                    shape = DotShape.Chunky,
                    litColor = if (on) colors.copperHot else colors.pewter,
                    unlitColor = Color.Transparent,
                    contentDescription = day.name,
                )
            }
        }
    }
}

val DayInitials: List<Pair<java.time.DayOfWeek, String>> = listOf(
    java.time.DayOfWeek.MONDAY to "M",
    java.time.DayOfWeek.TUESDAY to "T",
    java.time.DayOfWeek.WEDNESDAY to "W",
    java.time.DayOfWeek.THURSDAY to "T",
    java.time.DayOfWeek.FRIDAY to "F",
    java.time.DayOfWeek.SATURDAY to "S",
    java.time.DayOfWeek.SUNDAY to "S",
)
