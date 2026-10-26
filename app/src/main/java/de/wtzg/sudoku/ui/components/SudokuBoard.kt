package de.wtzg.sudoku.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import de.wtzg.sudoku.R
import de.wtzg.sudoku.data.GameSettings
import de.wtzg.sudoku.domain.GameState
import de.wtzg.sudoku.domain.Sudoku

private class DigitLayouts(
    val given: List<TextLayoutResult>,
    val entered: List<TextLayoutResult>,
    val note: List<TextLayoutResult>,
)

@Composable
fun SudokuBoard(
    game: GameState,
    selected: Int?,
    settings: GameSettings,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** Cells shaded to explain a technique. */
    marked: Set<Int> = emptySet(),
    /** Candidates (cell -> digit mask) shown emphasised, e.g. the digits of a pair. */
    markedNotes: Map<Int, Int> = emptyMap(),
    /** Candidates (cell -> digit mask) shown struck out because they can be removed. */
    removableNotes: Map<Int, Int> = emptyMap(),
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val shape = RoundedCornerShape(16.dp)
    val onClick by rememberUpdatedState(onCellClick)

    val selectedValue = selected?.let { game.values[it] }?.takeIf { it != 0 }

    val boardDescription = stringResource(R.string.board_description)
    BoxWithConstraints(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = boardDescription }
    ) {
        val cellPx = with(density) { maxWidth.toPx() } / 9f
        val layouts = remember(cellPx, density, textMeasurer) {
            val big = with(density) { (cellPx * 0.56f).toSp() }
            val small = with(density) { (cellPx * 0.26f).toSp() }
            fun measure(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight) =
                (1..9).map { textMeasurer.measure(it.toString(), TextStyle(fontSize = size, fontWeight = weight)) }
            DigitLayouts(
                given = measure(big, FontWeight.SemiBold),
                entered = measure(big, FontWeight.Normal),
                note = measure(small, FontWeight.Medium),
            )
        }

        val base = colors.surfaceContainerLowest
        val regionColor = colors.primary.copy(alpha = 0.08f).compositeOver(base)
        val sameColor = colors.primary.copy(alpha = 0.22f).compositeOver(base)
        val selectedColor = colors.primary.copy(alpha = 0.36f).compositeOver(base)
        val errorColor = colors.errorContainer
        val markedColor = colors.tertiary.copy(alpha = 0.24f).compositeOver(base)
        val thinLine = colors.outlineVariant
        val thickLine = colors.onSurface.copy(alpha = 0.75f)

        Canvas(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .pointerInput(Unit) {
                    detectTapGestures { o ->
                        val c = (o.x / (size.width / 9f)).toInt().coerceIn(0, 8)
                        val r = (o.y / (size.height / 9f)).toInt().coerceIn(0, 8)
                        onClick(r * 9 + c)
                    }
                }
        ) {
            val cell = size.width / 9f
            drawRect(base)

            for (i in 0 until Sudoku.CELLS) {
                val value = game.values[i]
                val invalid = if (settings.checkMistakes) game.isWrong(i) else game.hasConflict(i)
                val bg: Color? = when {
                    i == selected -> selectedColor
                    invalid && !game.isGiven(i) && !game.isQuit -> errorColor
                    i in marked -> markedColor
                    settings.highlightSameNumbers && selectedValue != null && value == selectedValue -> sameColor
                    settings.highlightRegion && selected != null && Sudoku.isRelated(i, selected) -> regionColor
                    else -> null
                }
                val x = Sudoku.col(i) * cell
                val y = Sudoku.row(i) * cell
                if (bg != null) drawRect(bg, Offset(x, y), Size(cell, cell))

                val revealed = game.isQuit && value != game.solution[i]
                if (revealed) {
                    val layout = layouts.entered[game.solution[i] - 1]
                    drawText(
                        layout,
                        color = colors.tertiary,
                        topLeft = Offset(
                            x + (cell - layout.size.width) / 2f,
                            y + (cell - layout.size.height) / 2f,
                        ),
                    )
                } else if (value != 0) {
                    val layout = if (game.isGiven(i)) layouts.given[value - 1] else layouts.entered[value - 1]
                    val color = when {
                        game.isGiven(i) -> colors.onSurface
                        invalid -> colors.error
                        else -> colors.primary
                    }
                    drawText(
                        layout,
                        color = color,
                        topLeft = Offset(
                            x + (cell - layout.size.width) / 2f,
                            y + (cell - layout.size.height) / 2f,
                        ),
                    )
                } else if (game.notes[i] != 0) {
                    val sub = cell / 3f
                    for (d in 1..9) {
                        if (!game.hasNote(i, d)) continue
                        val nx = x + ((d - 1) % 3) * sub
                        val ny = y + ((d - 1) / 3) * sub
                        val removable = (removableNotes[i] ?: 0) and (1 shl d) != 0
                        val key = (markedNotes[i] ?: 0) and (1 shl d) != 0
                        val highlighted = key || (settings.highlightSameNumbers && d == selectedValue)
                        if (removable) {
                            drawCircle(
                                colors.error.copy(alpha = 0.18f),
                                radius = sub * 0.46f,
                                center = Offset(nx + sub / 2f, ny + sub / 2f),
                            )
                        } else if (highlighted) {
                            drawCircle(
                                colors.primary.copy(alpha = 0.25f),
                                radius = sub * 0.46f,
                                center = Offset(nx + sub / 2f, ny + sub / 2f),
                            )
                        }
                        val layout = layouts.note[d - 1]
                        drawText(
                            layout,
                            color = when {
                                removable -> colors.error
                                highlighted -> colors.primary
                                else -> colors.onSurfaceVariant
                            },
                            topLeft = Offset(
                                nx + (sub - layout.size.width) / 2f,
                                ny + (sub - layout.size.height) / 2f,
                            ),
                        )
                    }
                }
            }

            val thin = 1.dp.toPx()
            val thick = 2.dp.toPx()
            for (k in 1..8) {
                if (k % 3 == 0) continue
                val p = k * cell
                drawLine(thinLine, Offset(p, 0f), Offset(p, size.height), thin)
                drawLine(thinLine, Offset(0f, p), Offset(size.width, p), thin)
            }
            for (k in listOf(3, 6)) {
                val p = k * cell
                drawLine(thickLine, Offset(p, 0f), Offset(p, size.height), thick)
                drawLine(thickLine, Offset(0f, p), Offset(size.width, p), thick)
            }
        }
        Box(Modifier.fillMaxSize().border(2.dp, thickLine, shape))
    }
}
