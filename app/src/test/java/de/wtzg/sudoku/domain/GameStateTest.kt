package de.wtzg.sudoku.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameStateTest {

    private val game = GameState.new(Difficulty.MEDIUM, Generator.generate(Difficulty.MEDIUM, Random(7)))

    @Test
    fun encodeDecodeRoundTrip() {
        val modified = game.copy(
            notes = game.notes.toMutableList().also { it[3] = 0b1010 },
            mistakes = 2,
            hintsUsed = 1,
            elapsedSeconds = 321,
        )
        assertEquals(modified, GameState.decode(modified.encode()))
    }

    @Test
    fun quitGamesAreInactiveAndOldSavesStillLoad() {
        val quit = game.copy(isQuit = true)
        assertFalse(quit.isActive)
        assertEquals(quit, GameState.decode(quit.encode()))
        // Saves written before the quit flag existed have 11 fields.
        val legacy = game.encode().substringBeforeLast(";")
        assertEquals(game, GameState.decode(legacy))
    }

    @Test
    fun decodeRejectsGarbage() {
        assertNull(GameState.decode("nope"))
        assertNull(GameState.decode(""))
    }

    @Test
    fun detectsWrongAndConflictingEntries() {
        val empty = game.givens.indexOfFirst { it == 0 }
        val wrongDigit = (1..9).first { it != game.solution[empty] }
        val g = game.copy(values = game.values.toMutableList().also { it[empty] = wrongDigit })
        assertTrue(g.isWrong(empty))
        assertFalse(game.isWrong(empty))
        assertEquals(0f, game.progress)
        assertEquals(1f, game.copy(values = game.solution).progress)
    }
}
