package de.wtzg.sudoku.ui

import androidx.annotation.StringRes
import de.wtzg.sudoku.R
import de.wtzg.sudoku.domain.Difficulty

fun formatTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@get:StringRes
val Difficulty.label: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
        Difficulty.EXPERT -> R.string.difficulty_expert
    }

@get:StringRes
val Difficulty.description: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy_desc
        Difficulty.MEDIUM -> R.string.difficulty_medium_desc
        Difficulty.HARD -> R.string.difficulty_hard_desc
        Difficulty.EXPERT -> R.string.difficulty_expert_desc
    }

/** 1..4, used to draw the little level bars. */
val Difficulty.level: Int get() = ordinal + 1
