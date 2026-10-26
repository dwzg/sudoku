package de.wtzg.sudoku.ui.learn

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.wtzg.sudoku.data.DrillBest
import de.wtzg.sudoku.data.LearnRepository
import de.wtzg.sudoku.domain.learn.Exercise
import de.wtzg.sudoku.domain.learn.ExerciseGenerator
import de.wtzg.sudoku.domain.learn.Finding
import de.wtzg.sudoku.domain.learn.Move
import de.wtzg.sudoku.domain.learn.Technique
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Feedback { CORRECT, WRONG, OTHER_TECHNIQUE, CANNOT_REMOVE, REVEALED }

/** One exercise being played: the board as it is now, plus how the player is doing. */
data class ExerciseUi(
    val exercise: Exercise,
    val values: List<Int> = exercise.values,
    val notes: List<Int> = exercise.candidates,
    val selected: Int? = null,
    val feedback: Feedback? = null,
    /** The finding the player completed (or that was revealed). Set once the exercise is done. */
    val solvedBy: Finding? = null,
    val mistakes: Int = 0,
) {
    val isDone get() = solvedBy != null
    val firstTry get() = isDone && mistakes == 0 && feedback != Feedback.REVEALED
}

data class LessonUi(
    val lesson: Lesson,
    val step: Int = 0,
    val exercise: ExerciseUi? = null,
    val loading: Boolean = false,
) {
    val exampleStep get() = lesson.pages.size
    val firstPracticeStep get() = exampleStep + 1
    val finishStep get() = firstPracticeStep + PRACTICE_ROUNDS
    val stepCount get() = finishStep + 1
}

data class TrainingUi(
    val drill: Drill,
    val round: Int = 1,
    val exercise: ExerciseUi? = null,
    val firstTryCount: Int = 0,
    val startedAt: Long = SystemClock.elapsedRealtime(),
    val finishedSeconds: Long? = null,
    val isNewBest: Boolean = false,
)

data class LearnState(
    val completedLessons: Set<String> = emptySet(),
    val drillBests: Map<String, DrillBest> = emptyMap(),
    val lesson: LessonUi? = null,
    val training: TrainingUi? = null,
)

class LearnViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LearnRepository(application)
    private val _state = MutableStateFlow(LearnState())
    val state: StateFlow<LearnState> = _state.asStateFlow()

    private var nextExercise: Deferred<Exercise>? = null
    private var job: Job? = null

    init {
        refresh()
    }

    private fun refresh() = _state.update {
        it.copy(
            completedLessons = repo.completedLessons(),
            drillBests = drills.associate { d -> d.id to repo.drillBest(d.id) },
        )
    }

    private fun prefetch(techniques: List<Technique>) {
        nextExercise = viewModelScope.async(Dispatchers.Default) { ExerciseGenerator.generateMixed(techniques) }
    }

    private suspend fun takeExercise(techniques: List<Technique>): Exercise {
        val ex = (nextExercise ?: viewModelScope.async(Dispatchers.Default) {
            ExerciseGenerator.generateMixed(techniques)
        }).await()
        prefetch(techniques)
        return ex
    }

    // region Lessons

    fun startLesson(lesson: Lesson) {
        job?.cancel()
        nextExercise = null
        prefetch(listOf(lesson.technique))
        _state.update { it.copy(lesson = LessonUi(lesson), training = null) }
    }

    fun lessonNext() {
        goToStep((_state.value.lesson ?: return).step + 1)
    }

    fun lessonBack() {
        goToStep((_state.value.lesson ?: return).step - 1)
    }

    private fun goToStep(step: Int) {
        val l = _state.value.lesson ?: return
        if (step !in 0 until l.stepCount) return
        if (step == l.finishStep) repo.markLessonDone(l.lesson.id).also { refresh() }

        val needsExercise = step in l.exampleStep until l.finishStep
        if (!needsExercise) {
            _state.update { it.copy(lesson = l.copy(step = step)) }
            return
        }
        _state.update { it.copy(lesson = l.copy(step = step, loading = true, exercise = null)) }
        job?.cancel()
        job = viewModelScope.launch {
            val ex = takeExercise(listOf(l.lesson.technique))
            var ui = ExerciseUi(ex)
            if (step == l.exampleStep) ui = reveal(ui)
            _state.update { s -> s.copy(lesson = s.lesson?.copy(exercise = ui, loading = false)) }
        }
    }

    // endregion

    // region Training

    fun startTraining(drill: Drill) {
        job?.cancel()
        nextExercise = null
        _state.update { it.copy(training = TrainingUi(drill), lesson = null) }
        loadRound()
    }

    fun restartTraining() {
        _state.value.training?.let { startTraining(it.drill) }
    }

    fun nextRound() {
        val t = _state.value.training ?: return
        val ex = t.exercise ?: return
        if (!ex.isDone || t.finishedSeconds != null) return
        val firstTry = t.firstTryCount + if (ex.firstTry) 1 else 0
        if (t.round >= t.drill.rounds) {
            val seconds = (SystemClock.elapsedRealtime() - t.startedAt) / 1000
            val best = repo.recordDrill(t.drill.id, firstTry, t.drill.rounds, seconds)
            refresh()
            _state.update { it.copy(training = t.copy(firstTryCount = firstTry, finishedSeconds = seconds, isNewBest = best)) }
            return
        }
        _state.update { it.copy(training = t.copy(round = t.round + 1, firstTryCount = firstTry, exercise = null)) }
        loadRound()
    }

    private fun loadRound() {
        val drill = _state.value.training?.drill ?: return
        job?.cancel()
        job = viewModelScope.launch {
            val ex = takeExercise(drill.techniques)
            _state.update { s -> s.copy(training = s.training?.copy(exercise = ExerciseUi(ex))) }
        }
    }

    fun close() {
        job?.cancel()
        nextExercise = null
        _state.update { it.copy(lesson = null, training = null) }
    }

    // endregion

    // region Exercise input (shared by lessons and training)

    fun select(cell: Int) = updateExercise { if (it.isDone) it else it.copy(selected = cell) }

    fun input(digit: Int) {
        val before = _state.value.training?.exercise
        updateExercise { ui -> answer(ui, digit) }
        // In training, a correct answer moves on by itself after a short moment.
        val current = _state.value.training?.exercise ?: return
        if (before?.isDone == false && current.feedback == Feedback.CORRECT) {
            viewModelScope.launch {
                delay(900)
                if (_state.value.training?.exercise === current) nextRound()
            }
        }
    }

    fun reveal() = updateExercise { if (it.isDone) it else reveal(it) }

    private fun answer(ui: ExerciseUi, digit: Int): ExerciseUi {
        val cell = ui.selected ?: return ui
        if (ui.isDone) return ui
        val ex = ui.exercise
        val move = Move(cell, digit)
        if (ex.technique.isPlacement) {
            if (ui.values[cell] != 0) return ui
            val finding = ex.findings.firstOrNull { move in it.placements }
            return when {
                finding != null -> apply(ui, finding, move).copy(feedback = Feedback.CORRECT)
                ex.solution[cell] == digit -> ui.copy(feedback = Feedback.OTHER_TECHNIQUE)
                else -> ui.copy(feedback = Feedback.WRONG, mistakes = ui.mistakes + 1)
            }
        } else {
            if (ui.notes[cell] and (1 shl digit) == 0) return ui
            val finding = ex.findings.firstOrNull { move in it.eliminations }
            return if (finding != null) apply(ui, finding, move).copy(feedback = Feedback.CORRECT)
            else ui.copy(feedback = Feedback.CANNOT_REMOVE, mistakes = ui.mistakes + 1)
        }
    }

    private fun reveal(ui: ExerciseUi): ExerciseUi {
        val finding = ui.exercise.findings.first()
        val move = finding.placements.firstOrNull() ?: finding.eliminations.first()
        return apply(ui, finding, move).copy(feedback = Feedback.REVEALED)
    }

    private fun apply(ui: ExerciseUi, finding: Finding, move: Move): ExerciseUi {
        return if (ui.exercise.technique.isPlacement) {
            ui.copy(values = ui.values.toMutableList().also { it[move.cell] = move.digit }, selected = move.cell, solvedBy = finding)
        } else {
            ui.copy(
                notes = ui.notes.toMutableList().also { it[move.cell] = it[move.cell] and (1 shl move.digit).inv() },
                selected = move.cell,
                solvedBy = finding,
            )
        }
    }

    private fun updateExercise(transform: (ExerciseUi) -> ExerciseUi) = _state.update { s ->
        when {
            s.training?.exercise != null && s.training.finishedSeconds == null ->
                s.copy(training = s.training.copy(exercise = transform(s.training.exercise)))
            s.lesson?.exercise != null -> s.copy(lesson = s.lesson.copy(exercise = transform(s.lesson.exercise)))
            else -> s
        }
    }

    // endregion
}
