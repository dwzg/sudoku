package de.wtzg.sudoku.ui.learn

import androidx.annotation.StringRes
import de.wtzg.sudoku.R
import de.wtzg.sudoku.domain.learn.Technique

class LessonPage(@StringRes val title: Int, @StringRes val body: Int)

class Lesson(
    val id: String,
    val technique: Technique,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val pages: List<LessonPage>,
)

class Drill(
    val id: String,
    val techniques: List<Technique>,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val rounds: Int,
)

const val PRACTICE_ROUNDS = 3

val lessons = listOf(
    Lesson(
        "rules", Technique.FULL_HOUSE, R.string.lesson_rules, R.string.lesson_rules_summary,
        listOf(
            LessonPage(R.string.lesson_rules_p1_title, R.string.lesson_rules_p1),
            LessonPage(R.string.lesson_rules_p2_title, R.string.lesson_rules_p2),
            LessonPage(R.string.lesson_rules_p3_title, R.string.lesson_rules_p3),
        ),
    ),
    Lesson(
        "hidden_single", Technique.HIDDEN_SINGLE, R.string.technique_hidden_single, R.string.lesson_hidden_single_summary,
        listOf(
            LessonPage(R.string.lesson_hidden_single_p1_title, R.string.lesson_hidden_single_p1),
            LessonPage(R.string.lesson_hidden_single_p2_title, R.string.lesson_hidden_single_p2),
            LessonPage(R.string.lesson_hidden_single_p3_title, R.string.lesson_hidden_single_p3),
        ),
    ),
    Lesson(
        "naked_single", Technique.NAKED_SINGLE, R.string.technique_naked_single, R.string.lesson_naked_single_summary,
        listOf(
            LessonPage(R.string.lesson_naked_single_p1_title, R.string.lesson_naked_single_p1),
            LessonPage(R.string.lesson_naked_single_p2_title, R.string.lesson_naked_single_p2),
        ),
    ),
    Lesson(
        "naked_pair", Technique.NAKED_PAIR, R.string.lesson_naked_pair, R.string.lesson_naked_pair_summary,
        listOf(
            LessonPage(R.string.lesson_naked_pair_p1_title, R.string.lesson_naked_pair_p1),
            LessonPage(R.string.lesson_naked_pair_p2_title, R.string.lesson_naked_pair_p2),
            LessonPage(R.string.lesson_naked_pair_p3_title, R.string.lesson_naked_pair_p3),
        ),
    ),
    Lesson(
        "pointing", Technique.POINTING, R.string.technique_pointing, R.string.lesson_pointing_summary,
        listOf(
            LessonPage(R.string.lesson_pointing_p1_title, R.string.lesson_pointing_p1),
            LessonPage(R.string.lesson_pointing_p2_title, R.string.lesson_pointing_p2),
        ),
    ),
    Lesson(
        "hidden_pair", Technique.HIDDEN_PAIR, R.string.technique_hidden_pair, R.string.lesson_hidden_pair_summary,
        listOf(
            LessonPage(R.string.lesson_hidden_pair_p1_title, R.string.lesson_hidden_pair_p1),
            LessonPage(R.string.lesson_hidden_pair_p2_title, R.string.lesson_hidden_pair_p2),
        ),
    ),
)

val drills = listOf(
    Drill("full_house", listOf(Technique.FULL_HOUSE), R.string.technique_full_house, R.string.drill_full_house_summary, 10),
    Drill("hidden_single", listOf(Technique.HIDDEN_SINGLE), R.string.technique_hidden_single, R.string.drill_hidden_single_summary, 10),
    Drill("naked_single", listOf(Technique.NAKED_SINGLE), R.string.technique_naked_single, R.string.drill_naked_single_summary, 10),
    Drill(
        "singles_mix",
        listOf(Technique.FULL_HOUSE, Technique.HIDDEN_SINGLE, Technique.NAKED_SINGLE),
        R.string.drill_singles_mix, R.string.drill_singles_mix_summary, 10,
    ),
    Drill("naked_pair", listOf(Technique.NAKED_PAIR), R.string.technique_naked_pair, R.string.drill_naked_pair_summary, 5),
    Drill("pointing", listOf(Technique.POINTING), R.string.technique_pointing, R.string.drill_pointing_summary, 5),
    Drill("hidden_pair", listOf(Technique.HIDDEN_PAIR), R.string.technique_hidden_pair, R.string.drill_hidden_pair_summary, 5),
)

@get:StringRes
val Technique.label: Int
    get() = when (this) {
        Technique.FULL_HOUSE -> R.string.technique_full_house
        Technique.HIDDEN_SINGLE -> R.string.technique_hidden_single
        Technique.NAKED_SINGLE -> R.string.technique_naked_single
        Technique.NAKED_PAIR -> R.string.technique_naked_pair
        Technique.POINTING -> R.string.technique_pointing
        Technique.HIDDEN_PAIR -> R.string.technique_hidden_pair
    }

@get:StringRes
val Technique.prompt: Int
    get() = when (this) {
        Technique.FULL_HOUSE -> R.string.prompt_full_house
        Technique.HIDDEN_SINGLE -> R.string.prompt_hidden_single
        Technique.NAKED_SINGLE -> R.string.prompt_naked_single
        Technique.NAKED_PAIR -> R.string.prompt_naked_pair
        Technique.POINTING -> R.string.prompt_pointing
        Technique.HIDDEN_PAIR -> R.string.prompt_hidden_pair
    }
