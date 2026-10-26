package de.wtzg.sudoku.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GeneratorTest {

    @Test
    fun generatedPuzzlesAreValidAndUnique() {
        for (difficulty in Difficulty.entries) {
            repeat(5) { seed ->
                val p = Generator.generate(difficulty, Random(seed))
                assertTrue(isValidSolution(p.solution))
                for (i in 0 until 81) {
                    if (p.givens[i] != 0) assertEquals(p.solution[i], p.givens[i])
                }
                assertEquals(1, Solver.countSolutions(p.givens))
                assertArrayEquals(p.solution, Solver.solve(p.givens))
                val clues = p.givens.count { it != 0 }
                assertTrue("$difficulty has $clues clues", clues <= difficulty.targetClues + 8)
            }
        }
    }

    @Test
    fun harderLevelsHaveFewerClues() {
        val avg = Difficulty.entries.map { d ->
            (0 until 5).map { Generator.generate(d, Random(it + 100)).givens.count { v -> v != 0 } }.average()
        }
        assertTrue(avg.zipWithNext().all { (a, b) -> a > b })
    }

    private fun isValidSolution(g: IntArray): Boolean {
        for (i in 0 until 81) {
            if (g[i] !in 1..9) return false
            if (Sudoku.peers[i].any { g[it] == g[i] }) return false
        }
        return true
    }
}
