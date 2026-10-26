package de.wtzg.sudoku.domain

const val MAX_MISTAKES = 3

/** Immutable snapshot of a game in progress. Notes are bitmasks: bit d set means note d. */
data class GameState(
    val difficulty: Difficulty,
    val givens: List<Int>,
    val solution: List<Int>,
    val values: List<Int>,
    val notes: List<Int>,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val elapsedSeconds: Long = 0,
    val isSolved: Boolean = false,
    val isFailed: Boolean = false,
) {
    val isActive get() = !isSolved && !isFailed

    fun isGiven(index: Int) = givens[index] != 0

    fun isWrong(index: Int) = values[index] != 0 && values[index] != solution[index]

    /** True if the cell's value is repeated in its row, column or box. */
    fun hasConflict(index: Int): Boolean {
        val v = values[index]
        return v != 0 && Sudoku.peers[index].any { values[it] == v }
    }

    fun hasNote(index: Int, digit: Int) = notes[index] and (1 shl digit) != 0

    fun placedCount(digit: Int) = values.count { it == digit }

    fun correctCount(digit: Int) = values.indices.count { values[it] == digit && solution[it] == digit }

    /** Fraction of the initially empty cells that are filled correctly. */
    val progress: Float
        get() {
            val open = givens.count { it == 0 }
            if (open == 0) return 1f
            val done = values.indices.count { givens[it] == 0 && values[it] == solution[it] }
            return done.toFloat() / open
        }

    fun encode(): String = listOf(
        VERSION,
        difficulty.name,
        givens.joinToString(""),
        solution.joinToString(""),
        values.joinToString(""),
        notes.joinToString(","),
        mistakes,
        hintsUsed,
        elapsedSeconds,
        isSolved,
        isFailed,
    ).joinToString(";")

    companion object {
        private const val VERSION = "v1"

        fun new(difficulty: Difficulty, puzzle: Puzzle) = GameState(
            difficulty = difficulty,
            givens = puzzle.givens.toList(),
            solution = puzzle.solution.toList(),
            values = puzzle.givens.toList(),
            notes = List(Sudoku.CELLS) { 0 },
        )

        fun decode(encoded: String): GameState? = runCatching {
            val p = encoded.split(";")
            require(p.size == 11 && p[0] == VERSION)
            fun digits(s: String) = s.map { it.digitToInt() }.also { require(it.size == Sudoku.CELLS) }
            GameState(
                difficulty = Difficulty.valueOf(p[1]),
                givens = digits(p[2]),
                solution = digits(p[3]),
                values = digits(p[4]),
                notes = p[5].split(",").map { it.toInt() }.also { require(it.size == Sudoku.CELLS) },
                mistakes = p[6].toInt(),
                hintsUsed = p[7].toInt(),
                elapsedSeconds = p[8].toLong(),
                isSolved = p[9].toBooleanStrict(),
                isFailed = p[10].toBooleanStrict(),
            )
        }.getOrNull()
    }
}
