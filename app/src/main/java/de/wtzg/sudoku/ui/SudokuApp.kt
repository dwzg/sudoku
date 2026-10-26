package de.wtzg.sudoku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.wtzg.sudoku.ui.screens.GameActions
import de.wtzg.sudoku.ui.screens.GameScreen
import de.wtzg.sudoku.ui.screens.HomeScreen
import de.wtzg.sudoku.ui.screens.SettingsScreen
import de.wtzg.sudoku.ui.screens.StatisticsScreen

@Composable
fun SudokuApp(vm: GameViewModel, state: UiState) {
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onForeground() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onBackground() }
    BackHandler(enabled = state.backStack.size > 1) { vm.back() }

    val gameActions = remember(vm) {
        GameActions(
            onBack = vm::back,
            onHome = vm::goHome,
            onSelect = vm::select,
            onDigit = vm::input,
            onErase = vm::erase,
            onUndo = vm::undo,
            onHint = vm::hint,
            onToggleNotes = vm::toggleNotesMode,
            onTogglePause = vm::togglePause,
            onRestart = vm::restart,
            onNewGame = vm::newGame,
            onSecondChance = vm::secondChance,
            onDismissResult = vm::dismissResult,
            onSettings = { vm.navigate(Screen.Settings) },
        )
    }

    AnimatedContent(
        targetState = state.screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
    ) { screen ->
        when (screen) {
            Screen.Home -> HomeScreen(
                state = state,
                onContinue = vm::continueGame,
                onNewGame = vm::newGame,
                onStatistics = { vm.navigate(Screen.Statistics) },
                onSettings = { vm.navigate(Screen.Settings) },
            )
            Screen.Game -> GameScreen(state, gameActions)
            Screen.Settings -> SettingsScreen(state.settings, onBack = vm::back, onChange = vm::updateSettings)
            Screen.Statistics -> StatisticsScreen(state.stats, onBack = vm::back, onReset = vm::resetStats)
        }
    }
}
