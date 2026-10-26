package de.wtzg.sudoku.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SolverTest {

    private fun parse(s: String) = s.filter { it.isDigit() || it == '.' }
        .map { if (it == '.') 0 else it.digitToInt() }.toIntArray()

    private val puzzle = parse(
        "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"
    )
    private val solution = parse(
        "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    )

    @Test
    fun solvesKnownPuzzle() {
        assertArrayEquals(solution, Solver.solve(puzzle))
    }

    @Test
    fun knownPuzzleIsUnique() {
        assertEquals(1, Solver.countSolutions(puzzle))
    }

    @Test
    fun emptyGridHasManySolutions() {
        assertEquals(2, Solver.countSolutions(IntArray(81), limit = 2))
    }

    @Test
    fun invalidGridHasNoSolution() {
        val bad = puzzle.copyOf().also { it[2] = 5 } // duplicates the 5 in row 0
        assertNull(Solver.solve(bad))
        assertEquals(0, Solver.countSolutions(bad))
    }
}
