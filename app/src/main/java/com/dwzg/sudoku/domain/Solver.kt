package com.dwzg.sudoku.domain

import kotlin.random.Random

/**
 * Backtracking solver using bitmasks and the "fewest candidates first" heuristic.
 * Grids are IntArrays of 81 values where 0 means empty.
 */
object Solver {

    /** Returns a solution, or null if the grid has none. Pass [random] to get a random solution. */
    fun solve(grid: IntArray, random: Random? = null): IntArray? {
        val search = Search(grid.copyOf(), random)
        if (!search.init()) return null
        search.run(limit = 1)
        return search.firstSolution
    }

    /** Counts solutions, stopping early once [limit] is reached. */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val search = Search(grid.copyOf(), null)
        if (!search.init()) return 0
        search.run(limit)
        return search.solutions
    }

    fun hasUniqueSolution(grid: IntArray) = countSolutions(grid, 2) == 1

    private const val ALL = 0x3FE // bits 1..9

    private class Search(val grid: IntArray, val random: Random?) {
        val rows = IntArray(9)
        val cols = IntArray(9)
        val boxes = IntArray(9)
        var solutions = 0
        var firstSolution: IntArray? = null

        fun init(): Boolean {
            for (i in 0 until Sudoku.CELLS) {
                val v = grid[i]
                if (v == 0) continue
                if (v !in 1..9) return false
                val bit = 1 shl v
                val r = Sudoku.row(i)
                val c = Sudoku.col(i)
                val b = Sudoku.box(i)
                if (rows[r] and bit != 0 || cols[c] and bit != 0 || boxes[b] and bit != 0) return false
                rows[r] = rows[r] or bit
                cols[c] = cols[c] or bit
                boxes[b] = boxes[b] or bit
            }
            return true
        }

        fun run(limit: Int) {
            var best = -1
            var bestMask = 0
            var bestCount = 10
            for (i in 0 until Sudoku.CELLS) {
                if (grid[i] != 0) continue
                val used = rows[Sudoku.row(i)] or cols[Sudoku.col(i)] or boxes[Sudoku.box(i)]
                val mask = ALL and used.inv()
                val count = Integer.bitCount(mask)
                if (count < bestCount) {
                    best = i
                    bestMask = mask
                    bestCount = count
                    if (count <= 1) break
                }
            }
            if (best == -1) {
                solutions++
                if (firstSolution == null) firstSolution = grid.copyOf()
                return
            }
            if (bestCount == 0) return

            val candidates = (1..9).filter { bestMask and (1 shl it) != 0 }
                .let { if (random != null) it.shuffled(random) else it }
            val r = Sudoku.row(best)
            val c = Sudoku.col(best)
            val b = Sudoku.box(best)
            for (v in candidates) {
                val bit = 1 shl v
                grid[best] = v
                rows[r] = rows[r] or bit
                cols[c] = cols[c] or bit
                boxes[b] = boxes[b] or bit
                run(limit)
                grid[best] = 0
                rows[r] = rows[r] and bit.inv()
                cols[c] = cols[c] and bit.inv()
                boxes[b] = boxes[b] and bit.inv()
                if (solutions >= limit) return
            }
        }
    }
}
