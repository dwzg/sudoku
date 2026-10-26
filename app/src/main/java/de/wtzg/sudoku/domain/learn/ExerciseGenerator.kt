package de.wtzg.sudoku.domain.learn

import de.wtzg.sudoku.domain.Difficulty
import de.wtzg.sudoku.domain.Generator
import de.wtzg.sudoku.domain.Sudoku
import kotlin.random.Random

/** A practice position: the board, its solution, and every way to apply [technique] on it. */
class Exercise(
    val technique: Technique,
    val values: List<Int>,
    val solution: List<Int>,
    val findings: List<Finding>,
) {
    /** Elimination exercises are played on a board with all candidates filled in. */
    val candidates: List<Int> =
        if (technique.isPlacement) List(Sudoku.CELLS) { 0 } else Techniques.candidates(values).toList()
}

/** Builds exercises from freshly generated puzzles. */
object ExerciseGenerator {

    fun generate(technique: Technique, random: Random = Random.Default): Exercise {
        repeat(200) {
            attempt(technique, random)?.let { return it }
        }
        error("Could not build an exercise for $technique")
    }

    /** Picks a random technique among [techniques] that has a finding on the generated board. */
    fun generateMixed(techniques: List<Technique>, random: Random = Random.Default): Exercise =
        generate(techniques.random(random), random)

    private fun attempt(technique: Technique, random: Random): Exercise? {
        val difficulty = if (technique.isPlacement) Difficulty.MEDIUM else Difficulty.HARD
        val puzzle = Generator.generate(difficulty, random)
        val solution = puzzle.solution.toList()
        val path = solvingPath(puzzle.givens.toList())

        if (technique == Technique.FULL_HOUSE) {
            // Take an early position and complete one house except for a single cell.
            val values = path[random.nextInt(minOf(path.size, 15))].toMutableList()
            val house = House.all.filter { h -> h.cells.count { values[it] == 0 } >= 3 }.randomOrNull(random)
                ?: return null
            val empty = house.cells.filter { values[it] == 0 }
            val keep = empty.random(random)
            empty.filter { it != keep }.forEach { values[it] = solution[it] }
            return build(technique, values, solution)
        }

        // Use positions with plenty of empty cells so the board isn't trivial.
        val minEmpty = if (technique.isPlacement) 30 else 25
        val candidates = path.filter { state -> state.count { it == 0 } >= minEmpty }.shuffled(random)
        for (values in candidates) {
            build(technique, values, solution)?.let { return it }
        }
        return null
    }

    private fun build(technique: Technique, values: List<Int>, solution: List<Int>): Exercise? {
        var findings = Techniques.find(technique, values)
        if (technique == Technique.NAKED_SINGLE) {
            // Prefer singles that can't also be found by the easier hidden-single scan.
            val hidden = Techniques.find(Technique.HIDDEN_SINGLE, values).flatMap { it.placements }.toSet()
            if (findings.none { it.placements[0] !in hidden }) return null
        }
        if (findings.isEmpty()) return null
        return Exercise(technique, values, solution, findings)
    }

    /** Board states from the givens onward, each one placing a single via the easiest technique. */
    private fun solvingPath(givens: List<Int>): List<List<Int>> {
        val states = mutableListOf(givens)
        var values = givens
        while (values.any { it == 0 }) {
            val move = Techniques.nextSingle(values)?.placements?.first() ?: break
            values = values.toMutableList().also { it[move.cell] = move.digit }
            states += values
        }
        return states
    }
}
