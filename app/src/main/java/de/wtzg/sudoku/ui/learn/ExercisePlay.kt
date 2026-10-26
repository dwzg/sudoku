package de.wtzg.sudoku.ui.learn

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wtzg.sudoku.R
import de.wtzg.sudoku.data.GameSettings
import de.wtzg.sudoku.domain.Difficulty
import de.wtzg.sudoku.domain.GameState
import de.wtzg.sudoku.domain.learn.Finding
import de.wtzg.sudoku.domain.learn.House
import de.wtzg.sudoku.domain.learn.HouseType
import de.wtzg.sudoku.domain.learn.Technique
import de.wtzg.sudoku.ui.components.NumberPad
import de.wtzg.sudoku.ui.components.SudokuBoard

private val exerciseSettings = GameSettings(checkMistakes = false)

/** Board, number pad and feedback for one exercise. [actions] sits below the pad (e.g. a Next button). */
@Composable
fun ExercisePlay(
    ui: ExerciseUi?,
    onSelect: (Int) -> Unit,
    onDigit: (Int) -> Unit,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val board = @Composable { m: Modifier -> ExerciseBoard(ui, onSelect, m) }
        val controls = @Composable {
            if (ui != null) {
                NumberPad(
                    remaining = { 0 },
                    notesMode = !ui.exercise.technique.isPlacement,
                    enabled = !ui.isDone,
                    onDigit = onDigit,
                    columns = if (landscape) 3 else 9,
                    showRemaining = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onReveal, enabled = ui != null && !ui.isDone) {
                    Text(stringResource(R.string.show_me))
                }
                actions()
            }
        }
        if (landscape) {
            Row(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                board(Modifier.fillMaxHeight().aspectRatio(1f))
                Column(
                    Modifier.widthIn(max = 380.dp).weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    header?.invoke()
                    FeedbackCard(ui)
                    controls()
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxHeight()) {
                    header?.invoke()
                    FeedbackCard(ui)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        board(Modifier.aspectRatio(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { controls() }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ExerciseBoard(ui: ExerciseUi?, onSelect: (Int) -> Unit, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (ui == null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            return@Box
        }
        val ex = ui.exercise
        val game = GameState(
            difficulty = Difficulty.EASY,
            givens = ex.values,
            solution = ex.solution,
            values = ui.values,
            notes = ui.notes,
        )
        val f = ui.solvedBy
        val marked = f?.let(::markedCells).orEmpty()
        SudokuBoard(
            game = game,
            selected = ui.selected,
            // While explaining, shade only the houses involved, not the selection's row/column/box.
            settings = exerciseSettings.copy(highlightRegion = marked.isEmpty()),
            onCellClick = onSelect,
            modifier = Modifier.fillMaxSize(),
            marked = marked,
            markedNotes = f?.let(::keyNotes).orEmpty(),
            removableNotes = f?.let { removable(it, ui.notes) }.orEmpty(),
        )
    }
}

private fun markedCells(f: Finding): Set<Int> = buildSet {
    f.house?.let { addAll(it.cells) }
    f.line?.let { addAll(it.cells) }
}

private fun keyNotes(f: Finding): Map<Int, Int> {
    if (f.technique.isPlacement) return emptyMap()
    val mask = f.digits.fold(0) { m, d -> m or (1 shl d) }
    return f.cells.associateWith { mask }
}

private fun removable(f: Finding, notes: List<Int>): Map<Int, Int> =
    f.eliminations.filter { notes[it.cell] and (1 shl it.digit) != 0 }
        .groupBy { it.cell }
        .mapValues { (_, moves) -> moves.fold(0) { m, mv -> m or (1 shl mv.digit) } }

@Composable
private fun FeedbackCard(ui: ExerciseUi?) {
    val colors = MaterialTheme.colorScheme
    val feedback = ui?.feedback
    val (container, content) = when (feedback) {
        Feedback.CORRECT -> colors.primaryContainer to colors.onPrimaryContainer
        Feedback.REVEALED -> colors.tertiaryContainer to colors.onTertiaryContainer
        Feedback.WRONG, Feedback.CANNOT_REMOVE -> colors.errorContainer to colors.onErrorContainer
        Feedback.OTHER_TECHNIQUE -> colors.secondaryContainer to colors.onSecondaryContainer
        null -> colors.surfaceContainer to colors.onSurface
    }
    val bg by animateColorAsState(container, label = "feedback")
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        contentColor = content,
        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (ui == null) {
                Text(stringResource(R.string.preparing_exercise), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            val technique = stringResource(ui.exercise.technique.label)
            val title = when (feedback) {
                Feedback.CORRECT -> stringResource(R.string.feedback_correct)
                Feedback.REVEALED -> stringResource(R.string.feedback_revealed)
                Feedback.WRONG -> stringResource(R.string.feedback_wrong)
                Feedback.CANNOT_REMOVE -> stringResource(R.string.feedback_cannot_remove)
                Feedback.OTHER_TECHNIQUE -> stringResource(R.string.feedback_other_technique)
                null -> technique
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val body = when {
                ui.solvedBy != null -> explanation(ui.solvedBy)
                feedback == Feedback.OTHER_TECHNIQUE -> stringResource(R.string.feedback_other_technique_body, technique)
                else -> stringResource(ui.exercise.technique.prompt)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Color.Unspecified)
        }
    }
}

@Composable
fun houseName(h: House): String = stringResource(
    when (h.type) {
        HouseType.ROW -> R.string.house_row
        HouseType.COLUMN -> R.string.house_column
        HouseType.BOX -> R.string.house_box
    },
    h.index + 1,
)

@Composable
private fun explanation(f: Finding): String {
    val house = f.house?.let { houseName(it) }.orEmpty()
    return when (f.technique) {
        Technique.FULL_HOUSE -> stringResource(R.string.explain_full_house, house, f.digits[0])
        Technique.HIDDEN_SINGLE -> stringResource(R.string.explain_hidden_single, house, f.digits[0])
        Technique.NAKED_SINGLE -> stringResource(R.string.explain_naked_single, f.digits[0])
        Technique.NAKED_PAIR -> stringResource(R.string.explain_naked_pair, house, f.digits[0], f.digits[1])
        Technique.POINTING -> stringResource(R.string.explain_pointing, house, f.digits[0], houseName(f.line!!))
        Technique.HIDDEN_PAIR -> stringResource(R.string.explain_hidden_pair, house, f.digits[0], f.digits[1])
    }
}
