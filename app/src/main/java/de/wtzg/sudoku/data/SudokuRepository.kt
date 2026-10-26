package de.wtzg.sudoku.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import de.wtzg.sudoku.domain.Difficulty
import de.wtzg.sudoku.domain.GameState

/** Stores the current game, settings and statistics in SharedPreferences. */
class SudokuRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)

    fun loadGame(): GameState? = prefs.getString(KEY_GAME, null)?.let(GameState::decode)

    fun saveGame(game: GameState?) = prefs.edit {
        if (game == null || !game.isActive) remove(KEY_GAME) else putString(KEY_GAME, game.encode())
    }

    fun loadSettings(): GameSettings {
        val d = GameSettings()
        return GameSettings(
            checkMistakes = prefs.getBoolean("check_mistakes", d.checkMistakes),
            mistakeLimit = prefs.getBoolean("mistake_limit", d.mistakeLimit),
            highlightRegion = prefs.getBoolean("highlight_region", d.highlightRegion),
            highlightSameNumbers = prefs.getBoolean("highlight_same", d.highlightSameNumbers),
            autoRemoveNotes = prefs.getBoolean("auto_remove_notes", d.autoRemoveNotes),
            themeMode = prefs.getString("theme_mode", null)
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
            dynamicColor = prefs.getBoolean("dynamic_color", d.dynamicColor),
        )
    }

    fun saveSettings(s: GameSettings) = prefs.edit {
        putBoolean("check_mistakes", s.checkMistakes)
        putBoolean("mistake_limit", s.mistakeLimit)
        putBoolean("highlight_region", s.highlightRegion)
        putBoolean("highlight_same", s.highlightSameNumbers)
        putBoolean("auto_remove_notes", s.autoRemoveNotes)
        putString("theme_mode", s.themeMode.name)
        putBoolean("dynamic_color", s.dynamicColor)
    }

    fun loadStats(): Map<Difficulty, DifficultyStats> =
        Difficulty.entries.associateWith { loadStats(it) }

    private fun loadStats(d: Difficulty) = DifficultyStats(
        played = prefs.getInt(key(d, "played"), 0),
        won = prefs.getInt(key(d, "won"), 0),
        bestSeconds = prefs.getLong(key(d, "best"), -1).takeIf { it >= 0 },
        totalWonSeconds = prefs.getLong(key(d, "total"), 0),
        currentStreak = prefs.getInt(key(d, "streak"), 0),
        bestStreak = prefs.getInt(key(d, "best_streak"), 0),
    )

    private fun saveStats(d: Difficulty, s: DifficultyStats) = prefs.edit {
        putInt(key(d, "played"), s.played)
        putInt(key(d, "won"), s.won)
        putLong(key(d, "best"), s.bestSeconds ?: -1)
        putLong(key(d, "total"), s.totalWonSeconds)
        putInt(key(d, "streak"), s.currentStreak)
        putInt(key(d, "best_streak"), s.bestStreak)
    }

    fun recordStarted(d: Difficulty) {
        val s = loadStats(d)
        saveStats(d, s.copy(played = s.played + 1))
    }

    /** Records a win and returns true if it's a new best time. */
    fun recordWon(d: Difficulty, seconds: Long): Boolean {
        val s = loadStats(d)
        val isBest = s.bestSeconds == null || seconds < s.bestSeconds
        val streak = s.currentStreak + 1
        saveStats(
            d, s.copy(
                won = s.won + 1,
                bestSeconds = if (isBest) seconds else s.bestSeconds,
                totalWonSeconds = s.totalWonSeconds + seconds,
                currentStreak = streak,
                bestStreak = maxOf(streak, s.bestStreak),
            )
        )
        return isBest
    }

    /** A lost or abandoned game breaks the win streak. */
    fun recordLost(d: Difficulty) {
        val s = loadStats(d)
        saveStats(d, s.copy(currentStreak = 0))
    }

    fun resetStats() = prefs.edit {
        Difficulty.entries.forEach { d ->
            listOf("played", "won", "best", "total", "streak", "best_streak").forEach { remove(key(d, it)) }
        }
    }

    private fun key(d: Difficulty, field: String) = "stats_${d.name}_$field"

    private companion object {
        const val KEY_GAME = "current_game"
    }
}
