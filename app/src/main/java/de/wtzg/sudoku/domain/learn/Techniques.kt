package de.wtzg.sudoku.domain.learn

import de.wtzg.sudoku.domain.Sudoku

/** Solving techniques taught in the learning section, from easiest to hardest. */
enum class Technique {
    FULL_HOUSE,
    HIDDEN_SINGLE,
    NAKED_SINGLE,
    NAKED_PAIR,
    POINTING,
    HIDDEN_PAIR;

    /** Placement techniques fill in a number; the others remove candidates. */
    val isPlacement: Boolean get() = this == FULL_HOUSE || this == HIDDEN_SINGLE || this == NAKED_SINGLE
}

enum class HouseType { ROW, COLUMN, BOX }

/** A row, column or box. [index] is 0-based. */
data class House(val type: HouseType, val index: Int) {
    val cells: List<Int>
        get() = when (type) {
            HouseType.ROW -> (0 until 9).map { index * 9 + it }
            HouseType.COLUMN -> (0 until 9).map { it * 9 + index }
            HouseType.BOX -> (0 until 9).map { (index / 3 * 3 + it / 3) * 9 + index % 3 * 3 + it % 3 }
        }

    companion object {
        val all: List<House> = HouseType.entries.flatMap { t -> (0 until 9).map { House(t, it) } }
        fun rowOf(cell: Int) = House(HouseType.ROW, Sudoku.row(cell))
        fun columnOf(cell: Int) = House(HouseType.COLUMN, Sudoku.col(cell))
        fun boxOf(cell: Int) = House(HouseType.BOX, Sudoku.box(cell))
    }
}

/** A cell and a digit, used for placements and candidate eliminations. */
data class Move(val cell: Int, val digit: Int)

/**
 * One application of a technique.
 * [house] is the unit the technique works in (null for a naked single), [line] the row or column a
 * pointing candidate points along, [cells] the key cells and [digits] the key digits.
 */
data class Finding(
    val technique: Technique,
    val house: House?,
    val digits: List<Int>,
    val cells: List<Int>,
    val placements: List<Move> = emptyList(),
    val eliminations: List<Move> = emptyList(),
    val line: House? = null,
)

object Techniques {

    private const val ALL = 0x3FE

    /** Candidate bitmasks (bit d = digit d possible) for every empty cell; 0 for filled cells. */
    fun candidates(values: List<Int>): IntArray = IntArray(Sudoku.CELLS) { i ->
        if (values[i] != 0) 0
        else {
            var used = 0
            for (p in Sudoku.peers[i]) used = used or (1 shl values[p])
            ALL and used.inv()
        }
    }

    fun find(technique: Technique, values: List<Int>, candidates: IntArray = candidates(values)): List<Finding> =
        when (technique) {
            Technique.FULL_HOUSE -> fullHouses(values)
            Technique.HIDDEN_SINGLE -> hiddenSingles(values, candidates)
            Technique.NAKED_SINGLE -> nakedSingles(values, candidates)
            Technique.NAKED_PAIR -> nakedPairs(values, candidates)
            Technique.POINTING -> pointing(values, candidates)
            Technique.HIDDEN_PAIR -> hiddenPairs(values, candidates)
        }

    /** The easiest placement available, used to step through a solving path. */
    fun nextSingle(values: List<Int>): Finding? {
        val c = candidates(values)
        return fullHouses(values).firstOrNull()
            ?: hiddenSingles(values, c).firstOrNull()
            ?: nakedSingles(values, c).firstOrNull()
    }

    private fun empties(house: House, values: List<Int>) = house.cells.filter { values[it] == 0 }

    private fun fullHouses(values: List<Int>): List<Finding> = House.all.mapNotNull { h ->
        val empty = empties(h, values)
        if (empty.size != 1) return@mapNotNull null
        val present = h.cells.fold(0) { m, c -> m or (1 shl values[c]) }
        val digit = (1..9).first { present and (1 shl it) == 0 }
        Finding(Technique.FULL_HOUSE, h, listOf(digit), empty, placements = listOf(Move(empty[0], digit)))
    }

    private fun hiddenSingles(values: List<Int>, cand: IntArray): List<Finding> = buildList {
        for (h in House.all) {
            val empty = empties(h, values)
            if (empty.size <= 1) continue // that's a full house
            for (d in 1..9) {
                val spots = empty.filter { cand[it] and (1 shl d) != 0 }
                if (spots.size == 1) {
                    add(Finding(Technique.HIDDEN_SINGLE, h, listOf(d), spots, placements = listOf(Move(spots[0], d))))
                }
            }
        }
    }

    private fun nakedSingles(values: List<Int>, cand: IntArray): List<Finding> =
        (0 until Sudoku.CELLS).filter { values[it] == 0 && Integer.bitCount(cand[it]) == 1 }.map { i ->
            val d = Integer.numberOfTrailingZeros(cand[i])
            Finding(Technique.NAKED_SINGLE, null, listOf(d), listOf(i), placements = listOf(Move(i, d)))
        }

    private fun digitsOf(mask: Int) = (1..9).filter { mask and (1 shl it) != 0 }

    private fun nakedPairs(values: List<Int>, cand: IntArray): List<Finding> = buildList {
        for (h in House.all) {
            val empty = empties(h, values)
            val pairs = empty.filter { Integer.bitCount(cand[it]) == 2 }.groupBy { cand[it] }
            for ((mask, cells) in pairs) {
                if (cells.size != 2) continue
                val elim = empty.filter { it !in cells }.flatMap { c ->
                    digitsOf(cand[c] and mask).map { Move(c, it) }
                }
                if (elim.isNotEmpty()) add(Finding(Technique.NAKED_PAIR, h, digitsOf(mask), cells, eliminations = elim))
            }
        }
    }

    private fun pointing(values: List<Int>, cand: IntArray): List<Finding> = buildList {
        for (b in 0 until 9) {
            val box = House(HouseType.BOX, b)
            for (d in 1..9) {
                val spots = box.cells.filter { values[it] == 0 && cand[it] and (1 shl d) != 0 }
                if (spots.size < 2) continue
                val lines = listOf(
                    spots.map { Sudoku.row(it) }.distinct().singleOrNull()?.let { House(HouseType.ROW, it) },
                    spots.map { Sudoku.col(it) }.distinct().singleOrNull()?.let { House(HouseType.COLUMN, it) },
                )
                for (line in lines.filterNotNull()) {
                    val elim = line.cells.filter {
                        Sudoku.box(it) != b && values[it] == 0 && cand[it] and (1 shl d) != 0
                    }.map { Move(it, d) }
                    if (elim.isNotEmpty()) {
                        add(Finding(Technique.POINTING, box, listOf(d), spots, eliminations = elim, line = line))
                    }
                }
            }
        }
    }

    private fun hiddenPairs(values: List<Int>, cand: IntArray): List<Finding> = buildList {
        for (h in House.all) {
            val empty = empties(h, values)
            val spots = (1..9).associateWith { d -> empty.filter { cand[it] and (1 shl d) != 0 } }
            for (d1 in 1..9) for (d2 in d1 + 1..9) {
                val s = spots.getValue(d1)
                if (s.size != 2 || s != spots.getValue(d2)) continue
                val keep = (1 shl d1) or (1 shl d2)
                val elim = s.flatMap { c -> digitsOf(cand[c] and keep.inv()).map { Move(c, it) } }
                if (elim.isNotEmpty()) add(Finding(Technique.HIDDEN_PAIR, h, listOf(d1, d2), s, eliminations = elim))
            }
        }
    }
}
