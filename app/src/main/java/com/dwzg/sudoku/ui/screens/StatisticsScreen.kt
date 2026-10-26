@file:OptIn(ExperimentalMaterial3Api::class)

package com.dwzg.sudoku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dwzg.sudoku.R
import com.dwzg.sudoku.data.DifficultyStats
import com.dwzg.sudoku.domain.Difficulty
import com.dwzg.sudoku.ui.formatTime
import com.dwzg.sudoku.ui.label

@Composable
fun StatisticsScreen(
    stats: Map<Difficulty, DifficultyStats>,
    onBack: () -> Unit,
    onReset: () -> Unit,
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { confirmReset = true }) {
                        Icon(Icons.Outlined.RestartAlt, stringResource(R.string.reset_stats))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Difficulty.entries.forEach { d ->
                StatsCard(d, stats[d] ?: DifficultyStats(), Modifier.widthIn(max = 560.dp))
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_stats)) },
            text = { Text(stringResource(R.string.reset_stats_message)) },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; onReset() }) { Text(stringResource(R.string.reset)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun StatsCard(difficulty: Difficulty, s: DifficultyStats, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(difficulty.label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.stat_played), s.played.toString(), Modifier.weight(1f))
                Stat(stringResource(R.string.stat_won), s.won.toString(), Modifier.weight(1f))
                Stat(stringResource(R.string.stat_win_rate), "${(s.winRate * 100).toInt()}%", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.stat_best_time), s.bestSeconds?.let(::formatTime) ?: "–", Modifier.weight(1f))
                Stat(stringResource(R.string.stat_avg_time), s.averageSeconds?.let(::formatTime) ?: "–", Modifier.weight(1f))
                Stat(stringResource(R.string.stat_streak), "${s.currentStreak} / ${s.bestStreak}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
