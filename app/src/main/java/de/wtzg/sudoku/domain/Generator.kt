package de.wtzg.sudoku.domain

import kotlin.random.Random

class Puzzle(val givens: IntArray, val solution: IntArray)

/** Generates puzzles that always have exactly one solution. */
object Generator {

    fun generate(difficulty: Difficulty, random: Random = Random.Default): Puzzle {
        val solution = requireNotNull(Solver.solve(IntArray(Sudoku.CELLS), random))
        val puzzle = solution.copyOf()
        var clues = Sudoku.CELLS

        // Remove cells in symmetric pairs, keeping each removal only if the solution stays unique.
        for (i in (0 until Sudoku.CELLS).shuffled(random)) {
            if (clues <= difficulty.targetClues) break
            val j = Sudoku.CELLS - 1 - i
            if (puzzle[i] == 0) continue
            val savedI = puzzle[i]
            val savedJ = puzzle[j]
            puzzle[i] = 0
            puzzle[j] = 0
            if (Solver.hasUniqueSolution(puzzle)) {
                clues -= if (i == j) 1 else 2
            } else {
                puzzle[i] = savedI
                puzzle[j] = savedJ
            }
        }
        return Puzzle(puzzle, solution)
    }
}
