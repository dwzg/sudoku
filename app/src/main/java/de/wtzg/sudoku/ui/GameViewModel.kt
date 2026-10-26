package de.wtzg.sudoku.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.wtzg.sudoku.data.DifficultyStats
import de.wtzg.sudoku.data.GameSettings
import de.wtzg.sudoku.data.SudokuRepository
import de.wtzg.sudoku.domain.Difficulty
import de.wtzg.sudoku.domain.GameState
import de.wtzg.sudoku.domain.Generator
import de.wtzg.sudoku.domain.MAX_MISTAKES
import de.wtzg.sudoku.domain.Sudoku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Home, Game, Settings, Statistics }

data class GameResult(
    val difficulty: Difficulty,
    val seconds: Long,
    val mistakes: Int,
    val hints: Int,
    val isNewBest: Boolean,
)

data class UiState(
    val backStack: List<Screen> = listOf(Screen.Home),
    val game: GameState? = null,
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val isPaused: Boolean = false,
    val isGenerating: Boolean = false,
    val canUndo: Boolean = false,
    val result: GameResult? = null,
    val settings: GameSettings = GameSettings(),
    val stats: Map<Difficulty, DifficultyStats> = emptyMap(),
) {
    val screen: Screen get() = backStack.last()
    val hasResumableGame: Boolean get() = game?.isActive == true
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SudokuRepository(application)

    private val _state = MutableStateFlow(
        UiState(
            game = repo.loadGame(),
            settings = repo.loadSettings(),
            stats = repo.loadStats(),
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private data class Snapshot(val values: List<Int>, val notes: List<Int>)

    private val undoStack = ArrayDeque<Snapshot>()
    private var inForeground = true

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _state.update { s ->
                    if (s.isTimerRunning()) s.copy(game = s.game!!.copy(elapsedSeconds = s.game.elapsedSeconds + 1)) else s
                }
            }
        }
    }

    private fun UiState.isTimerRunning() =
        inForeground && screen == Screen.Game && game?.isActive == true && !isPaused && !isGenerating

    // region Navigation

    fun navigate(screen: Screen) = _state.update { it.copy(backStack = it.backStack + screen) }

    fun back() {
        _state.update { if (it.backStack.size > 1) it.copy(backStack = it.backStack.dropLast(1)) else it }
        persist()
    }

    fun goHome() {
        _state.update { it.copy(backStack = listOf(Screen.Home), result = null) }
        persist()
    }

    // endregion

    // region Lifecycle

    fun onForeground() {
        inForeground = true
    }

    fun onBackground() {
        inForeground = false
        _state.update { if (it.screen == Screen.Game && it.game?.isActive == true) it.copy(isPaused = true) else it }
        persist()
    }

    // endregion

    // region Game setup

    fun newGame(difficulty: Difficulty) {
        if (_state.value.isGenerating) return
        _state.update { it.copy(isGenerating = true) }
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { Generator.generate(difficulty) }
            _state.value.game?.takeIf { it.isActive }?.let { repo.recordLost(it.difficulty) }
            repo.recordStarted(difficulty)
            undoStack.clear()
            _state.update {
                it.copy(
                    backStack = listOf(Screen.Home, Screen.Game),
                    game = GameState.new(difficulty, puzzle),
                    selected = null,
                    notesMode = false,
                    isPaused = false,
                    isGenerating = false,
                    canUndo = false,
                    result = null,
                    stats = repo.loadStats(),
                )
            }
            persist()
        }
    }

    fun continueGame() {
        if (!_state.value.hasResumableGame) return
        _state.update { it.copy(backStack = listOf(Screen.Home, Screen.Game), isPaused = false) }
    }

    fun restart() {
        val game = _state.value.game?.takeIf { it.isActive } ?: return
        undoStack.clear()
        _state.update {
            it.copy(
                game = game.copy(
                    values = game.givens,
                    notes = List(Sudoku.CELLS) { 0 },
                    mistakes = 0,
                    isFailed = false,
                ),
                selected = null,
                canUndo = false,
                isPaused = false,
            )
        }
        persist()
    }

    /** Ends the game as a loss. Stays on the board to reveal the solution, or goes home. */
    fun quitGame(showSolution: Boolean) {
        val game = _state.value.game?.takeIf { it.isActive } ?: return
        repo.recordLost(game.difficulty)
        undoStack.clear()
        _state.update {
            it.copy(
                game = game.copy(isQuit = true),
                selected = null,
                notesMode = false,
                isPaused = false,
                canUndo = false,
                stats = repo.loadStats(),
                backStack = if (showSolution) it.backStack else listOf(Screen.Home),
            )
        }
        persist()
    }

    /** After hitting the mistake limit, lets the player keep going with one more chance. */
    fun secondChance() {
        val game = _state.value.game ?: return
        _state.update { it.copy(game = game.copy(isFailed = false, mistakes = MAX_MISTAKES - 1)) }
        persist()
    }

    // endregion

    // region Moves

    fun select(index: Int) = _state.update { it.copy(selected = index) }

    fun toggleNotesMode() = _state.update { it.copy(notesMode = !it.notesMode) }

    fun togglePause() = _state.update { it.copy(isPaused = !it.isPaused) }

    fun input(digit: Int) {
        val s = _state.value
        val game = s.game ?: return
        val i = s.selected ?: return
        if (!game.isActive || s.isPaused || game.isGiven(i)) return

        if (s.notesMode) {
            if (game.values[i] != 0) return
            pushUndo(game)
            val notes = game.notes.toMutableList().also { it[i] = it[i] xor (1 shl digit) }
            _state.update { it.copy(game = game.copy(notes = notes), canUndo = true) }
            persist()
            return
        }

        if (game.values[i] == digit) return
        // A correct entry is final when mistakes are being checked.
        if (s.settings.checkMistakes && game.values[i] == game.solution[i]) return

        pushUndo(game)
        val wrong = digit != game.solution[i]
        val mistakes = if (wrong && s.settings.checkMistakes) game.mistakes + 1 else game.mistakes
        val failed = s.settings.checkMistakes && s.settings.mistakeLimit && mistakes >= MAX_MISTAKES
        applyPlacement(game.copy(mistakes = mistakes), i, digit, failed = failed)
    }

    fun erase() {
        val s = _state.value
        val game = s.game ?: return
        val i = s.selected ?: return
        if (!game.isActive || s.isPaused || game.isGiven(i)) return
        if (game.values[i] == 0 && game.notes[i] == 0) return
        if (s.settings.checkMistakes && game.values[i] == game.solution[i]) return
        pushUndo(game)
        val values = game.values.toMutableList().also { it[i] = 0 }
        val notes = game.notes.toMutableList().also { it[i] = 0 }
        _state.update { it.copy(game = game.copy(values = values, notes = notes), canUndo = true) }
        persist()
    }

    fun hint() {
        val s = _state.value
        val game = s.game ?: return
        if (!game.isActive || s.isPaused) return
        val target = s.selected?.takeIf { !game.isGiven(it) && game.values[it] != game.solution[it] }
            ?: game.values.indices.filter { game.values[it] != game.solution[it] }.randomOrNull()
            ?: return
        pushUndo(game)
        _state.update { it.copy(selected = target) }
        applyPlacement(game.copy(hintsUsed = game.hintsUsed + 1), target, game.solution[target], failed = false)
    }

    fun undo() {
        val game = _state.value.game ?: return
        if (!game.isActive || _state.value.isPaused) return
        val snapshot = undoStack.removeLastOrNull() ?: return
        _state.update {
            it.copy(
                game = game.copy(values = snapshot.values, notes = snapshot.notes),
                canUndo = undoStack.isNotEmpty(),
            )
        }
        persist()
    }

    private fun applyPlacement(game: GameState, index: Int, digit: Int, failed: Boolean) {
        val values = game.values.toMutableList().also { it[index] = digit }
        val notes = game.notes.toMutableList()
        notes[index] = 0
        if (_state.value.settings.autoRemoveNotes) {
            val mask = (1 shl digit).inv()
            for (p in Sudoku.peers[index]) notes[p] = notes[p] and mask
        }
        val solved = values == game.solution
        val updated = game.copy(values = values, notes = notes, isSolved = solved, isFailed = failed && !solved)

        var result: GameResult? = null
        if (solved) {
            val isBest = repo.recordWon(game.difficulty, game.elapsedSeconds)
            result = GameResult(game.difficulty, game.elapsedSeconds, game.mistakes, game.hintsUsed, isBest)
            undoStack.clear()
        } else if (updated.isFailed) {
            repo.recordLost(game.difficulty)
        }

        _state.update {
            it.copy(
                game = updated,
                canUndo = undoStack.isNotEmpty(),
                result = result ?: it.result,
                selected = if (solved) null else it.selected,
                stats = if (solved || updated.isFailed) repo.loadStats() else it.stats,
            )
        }
        persist()
    }

    private fun pushUndo(game: GameState) {
        undoStack.addLast(Snapshot(game.values, game.notes))
        if (undoStack.size > 200) undoStack.removeFirst()
    }

    // endregion

    // region Settings & stats

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val updated = transform(_state.value.settings)
        repo.saveSettings(updated)
        _state.update { it.copy(settings = updated) }
    }

    fun resetStats() {
        repo.resetStats()
        _state.update { it.copy(stats = repo.loadStats()) }
    }

    fun dismissResult() = _state.update { it.copy(result = null) }

    // endregion

    private fun persist() = repo.saveGame(_state.value.game)
}
