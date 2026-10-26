package de.wtzg.sudoku.data

import android.content.Context
import androidx.core.content.edit

data class DrillBest(val bestScore: Int = 0, val bestSeconds: Long? = null, val runs: Int = 0)

/** Stores completed lessons and best training results. */
class LearnRepository(context: Context) {

    private val prefs = context.getSharedPreferences("learn", Context.MODE_PRIVATE)

    fun completedLessons(): Set<String> = prefs.getStringSet("lessons_done", emptySet()).orEmpty()

    fun markLessonDone(id: String) = prefs.edit {
        putStringSet("lessons_done", completedLessons() + id)
    }

    fun drillBest(id: String) = DrillBest(
        bestScore = prefs.getInt("drill_${id}_score", 0),
        bestSeconds = prefs.getLong("drill_${id}_time", -1).takeIf { it >= 0 },
        runs = prefs.getInt("drill_${id}_runs", 0),
    )

    /** Saves a finished run. The best time only counts for runs with every round right first try. */
    fun recordDrill(id: String, score: Int, total: Int, seconds: Long): Boolean {
        val old = drillBest(id)
        val newBestTime = score == total && (old.bestSeconds == null || seconds < old.bestSeconds)
        prefs.edit {
            putInt("drill_${id}_runs", old.runs + 1)
            if (score > old.bestScore) putInt("drill_${id}_score", score)
            if (newBestTime) putLong("drill_${id}_time", seconds)
        }
        return newBestTime
    }
}
