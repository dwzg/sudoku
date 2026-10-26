package de.wtzg.sudoku

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.wtzg.sudoku.ui.GameViewModel
import de.wtzg.sudoku.ui.SudokuApp
import de.wtzg.sudoku.ui.theme.SudokuTheme
import de.wtzg.sudoku.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: GameViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            val dark = isDarkTheme(state.settings.themeMode)

            // Keep status/navigation bar icons readable when the in-app theme differs from the system one.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            SudokuTheme(darkTheme = dark, dynamicColor = state.settings.dynamicColor) {
                SudokuApp(vm, state)
            }
        }
    }
}
