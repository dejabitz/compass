package com.dejabit.compass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dejabit.compass.presentation.CompassViewModel
import com.dejabit.compass.ui.CompassScreen
import com.dejabit.compass.ui.theme.CompassTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CompassViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            CompassTheme {
                CompassScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
}
