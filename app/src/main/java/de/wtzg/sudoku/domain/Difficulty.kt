package de.wtzg.sudoku.domain

/** Difficulty is driven by how many clues stay on the board. */
enum class Difficulty(val targetClues: Int) {
    EASY(40),
    MEDIUM(33),
    HARD(28),
    EXPERT(24),
}
