package com.dwzg.sudoku.data

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class GameSettings(
    /** Reveal wrong entries (compared to the solution) and count them as mistakes. */
    val checkMistakes: Boolean = true,
    /** End the game after [com.dwzg.sudoku.domain.MAX_MISTAKES] mistakes. */
    val mistakeLimit: Boolean = false,
    val highlightRegion: Boolean = true,
    val highlightSameNumbers: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)

data class DifficultyStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestSeconds: Long? = null,
    val totalWonSeconds: Long = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
) {
    val winRate: Float get() = if (played == 0) 0f else won.toFloat() / played
    val averageSeconds: Long? get() = if (won == 0) null else totalWonSeconds / won
}
