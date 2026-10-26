package de.wtzg.sudoku.domain.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TechniquesTest {

    @Test
    fun exercisesExistForEveryTechniqueAndAreSound() {
        for (t in Technique.entries) {
            val start = System.currentTimeMillis()
            repeat(10) { seed ->
                val ex = ExerciseGenerator.generate(t, Random(seed * 31 + t.ordinal))
                assertTrue(ex.findings.isNotEmpty())
                // Every board value agrees with the solution.
                ex.values.forEachIndexed { i, v -> if (v != 0) assertEquals(ex.solution[i], v) }
                for (f in ex.findings) {
                    assertEquals(t, f.technique)
                    f.placements.forEach { assertEquals(ex.solution[it.cell], it.digit) }
                    f.eliminations.forEach { m ->
                        assertNotEquals(ex.solution[m.cell], m.digit)
                        assertTrue(ex.candidates[m.cell] and (1 shl m.digit) != 0)
                    }
                    assertTrue(f.placements.isNotEmpty() || f.eliminations.isNotEmpty())
                }
            }
            println("$t: ${(System.currentTimeMillis() - start) / 10} ms per exercise")
        }
    }

    @Test
    fun houseCellsAreCorrect() {
        assertEquals(listOf(0, 1, 2, 9, 10, 11, 18, 19, 20), House(HouseType.BOX, 0).cells)
        assertEquals(listOf(60, 61, 62, 69, 70, 71, 78, 79, 80), House(HouseType.BOX, 8).cells)
        assertEquals((0 until 9).map { it * 9 + 4 }, House(HouseType.COLUMN, 4).cells)
    }
}
