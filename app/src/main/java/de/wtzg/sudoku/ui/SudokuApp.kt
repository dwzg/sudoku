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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import de.wtzg.sudoku.ui.learn.LearnScreen
import de.wtzg.sudoku.ui.learn.LearnViewModel
import de.wtzg.sudoku.ui.learn.LessonScreen
import de.wtzg.sudoku.ui.learn.TrainingScreen
import de.wtzg.sudoku.ui.screens.GameActions
import de.wtzg.sudoku.ui.screens.GameScreen
import de.wtzg.sudoku.ui.screens.HomeScreen
import de.wtzg.sudoku.ui.screens.SettingsScreen
import de.wtzg.sudoku.ui.screens.StatisticsScreen

@Composable
fun SudokuApp(vm: GameViewModel, state: UiState) {
    val learnVm: LearnViewModel = viewModel()
    val learn by learnVm.state.collectAsStateWithLifecycle()
    val closeLearning = { learnVm.close(); vm.back() }

    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onForeground() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onBackground() }
    BackHandler(enabled = state.backStack.size > 1) {
        if (state.screen == Screen.Lesson || state.screen == Screen.Training) closeLearning() else vm.back()
    }

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
            onQuit = vm::quitGame,
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
                onLearn = { vm.navigate(Screen.Learn) },
            )
            Screen.Game -> GameScreen(state, gameActions)
            Screen.Settings -> SettingsScreen(state.settings, onBack = vm::back, onChange = vm::updateSettings)
            Screen.Statistics -> StatisticsScreen(state.stats, onBack = vm::back, onReset = vm::resetStats)
            Screen.Learn -> LearnScreen(
                state = learn,
                onBack = vm::back,
                onLesson = { learnVm.startLesson(it); vm.navigate(Screen.Lesson) },
                onDrill = { learnVm.startTraining(it); vm.navigate(Screen.Training) },
            )
            Screen.Lesson -> LessonScreen(
                lessonUi = learn.lesson,
                vm = learnVm,
                onClose = closeLearning,
                onTrain = { learnVm.startTraining(it); vm.replace(Screen.Training) },
            )
            Screen.Training -> TrainingScreen(learn.training, learnVm, onClose = closeLearning)
        }
    }
}
