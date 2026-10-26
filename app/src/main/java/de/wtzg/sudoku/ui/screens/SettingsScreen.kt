@file:OptIn(ExperimentalMaterial3Api::class)

package de.wtzg.sudoku.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.wtzg.sudoku.R
import de.wtzg.sudoku.data.GameSettings
import de.wtzg.sudoku.data.ThemeMode

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onBack: () -> Unit,
    onChange: ((GameSettings) -> GameSettings) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.section_gameplay))
            SwitchRow(
                stringResource(R.string.setting_check_mistakes),
                stringResource(R.string.setting_check_mistakes_desc),
                settings.checkMistakes,
            ) { v -> onChange { it.copy(checkMistakes = v) } }
            SwitchRow(
                stringResource(R.string.setting_mistake_limit),
                stringResource(R.string.setting_mistake_limit_desc),
                settings.mistakeLimit,
                enabled = settings.checkMistakes,
            ) { v -> onChange { it.copy(mistakeLimit = v) } }
            SwitchRow(
                stringResource(R.string.setting_auto_notes),
                stringResource(R.string.setting_auto_notes_desc),
                settings.autoRemoveNotes,
            ) { v -> onChange { it.copy(autoRemoveNotes = v) } }

            SectionHeader(stringResource(R.string.section_highlights))
            SwitchRow(
                stringResource(R.string.setting_highlight_region),
                stringResource(R.string.setting_highlight_region_desc),
                settings.highlightRegion,
            ) { v -> onChange { it.copy(highlightRegion = v) } }
            SwitchRow(
                stringResource(R.string.setting_highlight_same),
                stringResource(R.string.setting_highlight_same_desc),
                settings.highlightSameNumbers,
            ) { v -> onChange { it.copy(highlightSameNumbers = v) } }

            SectionHeader(stringResource(R.string.section_appearance))
            val modes = ThemeMode.entries
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { onChange { it.copy(themeMode = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    ) {
                        Text(
                            stringResource(
                                when (mode) {
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.DARK -> R.string.theme_dark
                                }
                            )
                        )
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(
                    stringResource(R.string.setting_dynamic_color),
                    stringResource(R.string.setting_dynamic_color_desc),
                    settings.dynamicColor,
                ) { v -> onChange { it.copy(dynamicColor = v) } }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked && enabled, onCheckedChange = null, enabled = enabled) },
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}
