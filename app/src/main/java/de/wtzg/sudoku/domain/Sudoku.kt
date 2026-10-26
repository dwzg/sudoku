package de.wtzg.sudoku.domain

/** Grid geometry helpers. Cells are indexed 0..80, row-major. */
object Sudoku {
    const val SIZE = 9
    const val CELLS = 81

    fun row(index: Int) = index / SIZE
    fun col(index: Int) = index % SIZE
    fun box(index: Int) = (row(index) / 3) * 3 + col(index) / 3

    /** For every cell, the 20 other cells that share its row, column or box. */
    val peers: Array<IntArray> = Array(CELLS) { i ->
        (0 until CELLS).filter { j ->
            j != i && (row(j) == row(i) || col(j) == col(i) || box(j) == box(i))
        }.toIntArray()
    }

    fun isRelated(a: Int, b: Int) =
        row(a) == row(b) || col(a) == col(b) || box(a) == box(b)
}
