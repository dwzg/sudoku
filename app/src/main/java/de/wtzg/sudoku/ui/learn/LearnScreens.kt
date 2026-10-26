@file:OptIn(ExperimentalMaterial3Api::class)

package de.wtzg.sudoku.ui.learn

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wtzg.sudoku.R
import de.wtzg.sudoku.data.DrillBest
import de.wtzg.sudoku.ui.formatTime
import kotlinx.coroutines.delay

// region Overview

@Composable
fun LearnScreen(
    state: LearnState,
    onBack: () -> Unit,
    onLesson: (Lesson) -> Unit,
    onDrill: (Drill) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.learn)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionTitle(stringResource(R.string.lessons), stringResource(R.string.lessons_summary)) }
            items(lessons.size) { i ->
                val lesson = lessons[i]
                LessonCard(i + 1, lesson, lesson.id in state.completedLessons) { onLesson(lesson) }
            }
            item {
                Spacer(Modifier.height(12.dp))
                SectionTitle(stringResource(R.string.training), stringResource(R.string.training_summary))
            }
            items(drills, key = { it.id }) { drill ->
                DrillCard(drill, state.drillBests[drill.id] ?: DrillBest()) { onDrill(drill) }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(start = 4.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ListCard(onClick: () -> Unit, leading: @Composable () -> Unit, title: String, subtitle: String) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            leading()
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LessonCard(number: Int, lesson: Lesson, done: Boolean, onClick: () -> Unit) {
    ListCard(
        onClick = onClick,
        leading = {
            Surface(
                shape = CircleShape,
                color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (done) Icon(Icons.Filled.Check, stringResource(R.string.completed), tint = MaterialTheme.colorScheme.onPrimary)
                    else Text(
                        number.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        title = stringResource(lesson.title),
        subtitle = stringResource(lesson.summary),
    )
}

@Composable
private fun DrillCard(drill: Drill, best: DrillBest, onClick: () -> Unit) {
    val result = when {
        best.runs == 0 -> stringResource(R.string.drill_not_played)
        best.bestSeconds != null ->
            stringResource(R.string.drill_best_time, best.bestScore, drill.rounds, formatTime(best.bestSeconds))
        else -> stringResource(R.string.drill_best_score, best.bestScore, drill.rounds)
    }
    ListCard(
        onClick = onClick,
        leading = {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.FitnessCenter, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        },
        title = stringResource(drill.title),
        subtitle = "${stringResource(drill.summary)}\n$result",
    )
}

// endregion

// region Lesson

@Composable
fun LessonScreen(
    lessonUi: LessonUi?,
    vm: LearnViewModel,
    onClose: () -> Unit,
    onTrain: (Drill) -> Unit,
) {
    val l = lessonUi ?: return
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(l.lesson.title)) },
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
                    },
                )
                LinearProgressIndicator(
                    progress = { (l.step + 1f) / l.stepCount },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    drawStopIndicator = {},
                )
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = l.step,
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "lesson-step",
        ) { step ->
            val isExercise = step in l.exampleStep until l.finishStep
            when {
                step < l.exampleStep -> PageStep(l, step, vm)
                isExercise -> {
                    val label = if (step == l.exampleStep) stringResource(R.string.example)
                    else stringResource(R.string.practice_n, step - l.firstPracticeStep + 1, PRACTICE_ROUNDS)
                    ExercisePlay(
                        ui = l.exercise.takeIf { l.step == step },
                        onSelect = vm::select,
                        onDigit = vm::input,
                        onReveal = vm::reveal,
                        modifier = Modifier.padding(top = 12.dp),
                        header = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                            )
                        },
                        actions = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = vm::lessonBack) { Text(stringResource(R.string.previous)) }
                                Button(onClick = vm::lessonNext, enabled = l.exercise?.isDone == true) {
                                    Text(stringResource(R.string.next))
                                }
                            }
                        },
                    )
                }
                else -> FinishStep(l, onClose, onTrain)
            }
        }
    }
}

@Composable
private fun PageStep(l: LessonUi, step: Int, vm: LearnViewModel) {
    val page = l.lesson.pages[step]
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.weight(1f).widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.School, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Text(
                        stringResource(R.string.step_n, step + 1, l.lesson.pages.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(page.title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(page.body), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Row(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            if (step > 0) OutlinedButton(onClick = vm::lessonBack) { Text(stringResource(R.string.previous)) }
            Button(onClick = vm::lessonNext) { Text(stringResource(R.string.next)) }
        }
    }
}

@Composable
private fun FinishStep(l: LessonUi, onClose: () -> Unit, onTrain: (Drill) -> Unit) {
    val drill = drills.firstOrNull { it.techniques == listOf(l.lesson.technique) }
    CenteredMessage(
        title = stringResource(R.string.lesson_complete),
        body = stringResource(R.string.lesson_complete_body, stringResource(l.lesson.title)),
    ) {
        if (drill != null) Button(onClick = { onTrain(drill) }) { Text(stringResource(R.string.train_this)) }
        FilledTonalButton(onClick = onClose) { Text(stringResource(R.string.back_to_learn)) }
    }
}

@Composable
private fun CenteredMessage(title: String, body: String, buttons: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(88.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.EmojiEvents, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(Modifier.height(28.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            buttons()
        }
    }
}

// endregion

// region Training

@Composable
fun TrainingScreen(
    training: TrainingUi?,
    vm: LearnViewModel,
    onClose: () -> Unit,
) {
    val t = training ?: return
    val seconds by produceState(0L, t.startedAt, t.finishedSeconds) {
        if (t.finishedSeconds != null) {
            value = t.finishedSeconds
            return@produceState
        }
        while (true) {
            value = (android.os.SystemClock.elapsedRealtime() - t.startedAt) / 1000
            delay(250)
        }
    }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(t.drill.title))
                            if (t.finishedSeconds == null) {
                                Text(
                                    stringResource(R.string.round_n, t.round, t.drill.rounds),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
                    },
                    actions = {
                        Text(
                            formatTime(seconds),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    },
                )
                LinearProgressIndicator(
                    progress = {
                        val done = (t.round - 1) + if (t.exercise?.isDone == true || t.finishedSeconds != null) 1 else 0
                        done.toFloat() / t.drill.rounds
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    drawStopIndicator = {},
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (t.finishedSeconds != null) {
                val body = buildString {
                    append(stringResource(R.string.training_result, t.firstTryCount, t.drill.rounds, formatTime(t.finishedSeconds)))
                    if (t.isNewBest) append("\n\n").append(stringResource(R.string.new_best_time))
                }
                CenteredMessage(stringResource(R.string.training_done), body) {
                    Button(onClick = vm::restartTraining) { Text(stringResource(R.string.train_again)) }
                    FilledTonalButton(onClick = onClose) { Text(stringResource(R.string.back_to_learn)) }
                }
            } else {
                ExercisePlay(
                    ui = t.exercise,
                    onSelect = vm::select,
                    onDigit = vm::input,
                    onReveal = vm::reveal,
                    modifier = Modifier.padding(top = 12.dp),
                    actions = {
                        Button(onClick = vm::nextRound, enabled = t.exercise?.isDone == true) {
                            Text(stringResource(if (t.round == t.drill.rounds) R.string.finish else R.string.next))
                        }
                    },
                )
            }
        }
    }
}

// endregion
