@file:OptIn(ExperimentalMaterial3Api::class)

package com.dwzg.sudoku.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dwzg.sudoku.R
import com.dwzg.sudoku.domain.Difficulty
import com.dwzg.sudoku.domain.GameState
import com.dwzg.sudoku.domain.MAX_MISTAKES
import com.dwzg.sudoku.ui.GameResult
import com.dwzg.sudoku.ui.UiState
import com.dwzg.sudoku.ui.components.NumberPad
import com.dwzg.sudoku.ui.components.SudokuBoard
import com.dwzg.sudoku.ui.components.ToolButton
import com.dwzg.sudoku.ui.formatTime
import com.dwzg.sudoku.ui.label

class GameActions(
    val onBack: () -> Unit,
    val onHome: () -> Unit,
    val onSelect: (Int) -> Unit,
    val onDigit: (Int) -> Unit,
    val onErase: () -> Unit,
    val onUndo: () -> Unit,
    val onHint: () -> Unit,
    val onToggleNotes: () -> Unit,
    val onTogglePause: () -> Unit,
    val onRestart: () -> Unit,
    val onNewGame: (Difficulty) -> Unit,
    val onSecondChance: () -> Unit,
    val onDismissResult: () -> Unit,
    val onSettings: () -> Unit,
)

@Composable
fun GameScreen(state: UiState, actions: GameActions) {
    val game = state.game ?: return
    var menuOpen by remember { mutableStateOf(false) }
    var confirmRestart by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(game.difficulty.label), fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (game.isActive) {
                        IconButton(onClick = actions.onTogglePause) {
                            if (state.isPaused) Icon(Icons.Filled.PlayArrow, stringResource(R.string.resume))
                            else Icon(Icons.Filled.Pause, stringResource(R.string.pause))
                        }
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, stringResource(R.string.more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (game.isActive) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.restart)) },
                                    onClick = { menuOpen = false; confirmRestart = true },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.new_game)) },
                                onClick = { menuOpen = false; actions.onNewGame(game.difficulty) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings)) },
                                onClick = { menuOpen = false; actions.onSettings() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BoardArea(state, game, actions, Modifier.fillMaxHeight().aspectRatio(1f))
                    Column(
                        Modifier.widthIn(max = 360.dp).weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        InfoBar(game, state)
                        Tools(state, actions)
                        Pad(state, game, actions, columns = 3)
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.widthIn(max = 560.dp).fillMaxHeight()) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                InfoBar(game, state)
                                Spacer(Modifier.height(12.dp))
                                BoardArea(
                                    state, game, actions,
                                    Modifier
                                        .weight(1f, fill = false)
                                        .aspectRatio(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Tools(state, actions)
                        Spacer(Modifier.height(20.dp))
                        Pad(state, game, actions, columns = 9)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text(stringResource(R.string.restart_title)) },
            text = { Text(stringResource(R.string.restart_message)) },
            confirmButton = {
                TextButton(onClick = { confirmRestart = false; actions.onRestart() }) {
                    Text(stringResource(R.string.restart))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    state.result?.let { ResultDialog(it, actions) }

    if (game.isFailed) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Outlined.SentimentDissatisfied, null) },
            title = { Text(stringResource(R.string.game_over)) },
            text = { Text(pluralStringResource(R.plurals.game_over_message, MAX_MISTAKES, MAX_MISTAKES)) },
            confirmButton = {
                Button(onClick = actions.onSecondChance) { Text(stringResource(R.string.second_chance)) }
            },
            dismissButton = {
                TextButton(onClick = { actions.onNewGame(game.difficulty) }) {
                    Text(stringResource(R.string.new_game))
                }
            },
        )
    }
}

@Composable
private fun InfoBar(game: GameState, state: UiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val mistakes = when {
            !state.settings.checkMistakes -> "–"
            state.settings.mistakeLimit -> "${game.mistakes}/$MAX_MISTAKES"
            else -> game.mistakes.toString()
        }
        InfoItem(stringResource(R.string.mistakes), mistakes, Alignment.Start)
        InfoItem(stringResource(R.string.hints), game.hintsUsed.toString(), Alignment.CenterHorizontally)
        InfoItem(stringResource(R.string.time), formatTime(game.elapsedSeconds), Alignment.End)
    }
}

@Composable
private fun InfoItem(label: String, value: String, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BoardArea(state: UiState, game: GameState, actions: GameActions, modifier: Modifier) {
    AnimatedContent(targetState = state.isPaused && game.isActive, modifier = modifier, label = "board") { paused ->
        if (paused) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.paused),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(16.dp))
                    FilledIconButton(onClick = actions.onTogglePause, modifier = Modifier.height(56.dp).aspectRatio(1f)) {
                        Icon(Icons.Filled.PlayArrow, stringResource(R.string.resume))
                    }
                }
            }
        } else {
            SudokuBoard(
                game = game,
                selected = state.selected,
                settings = state.settings,
                onCellClick = actions.onSelect,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Tools(state: UiState, actions: GameActions) {
    val enabled = state.game?.isActive == true && !state.isPaused
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ToolButton(
            Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.undo), actions.onUndo,
            enabled = enabled && state.canUndo,
        )
        ToolButton(
            Icons.AutoMirrored.Outlined.Backspace, stringResource(R.string.erase), actions.onErase,
            enabled = enabled,
        )
        ToolButton(
            Icons.Outlined.Edit, stringResource(R.string.notes), actions.onToggleNotes,
            enabled = enabled,
            active = state.notesMode,
            badge = stringResource(if (state.notesMode) R.string.on else R.string.off),
        )
        ToolButton(
            Icons.Outlined.Lightbulb, stringResource(R.string.hint), actions.onHint,
            enabled = enabled,
        )
    }
}

@Composable
private fun Pad(state: UiState, game: GameState, actions: GameActions, columns: Int) {
    NumberPad(
        remaining = { d ->
            9 - if (state.settings.checkMistakes) game.correctCount(d) else game.placedCount(d)
        },
        notesMode = state.notesMode,
        enabled = game.isActive && !state.isPaused,
        onDigit = actions.onDigit,
        columns = columns,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ResultDialog(result: GameResult, actions: GameActions) {
    AlertDialog(
        onDismissRequest = actions.onDismissResult,
        icon = {
            Icon(Icons.Outlined.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text(stringResource(R.string.solved_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.isNewBest) {
                    Text(
                        stringResource(R.string.new_best_time),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                ResultRow(stringResource(R.string.difficulty), stringResource(result.difficulty.label))
                ResultRow(stringResource(R.string.time), formatTime(result.seconds))
                ResultRow(stringResource(R.string.mistakes), result.mistakes.toString())
                ResultRow(stringResource(R.string.hints), result.hints.toString())
            }
        },
        confirmButton = {
            Button(onClick = { actions.onNewGame(result.difficulty) }) {
                Text(stringResource(R.string.new_game))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onHome) { Text(stringResource(R.string.home)) }
        },
    )
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
