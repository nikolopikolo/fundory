package com.fundory.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.fundory.app.data.preferences.UserPreferencesRepository
import com.fundory.app.ui.navigation.FundoryNavHost
import com.fundory.app.ui.navigation.Screen
import com.fundory.app.ui.theme.FundoryTheme
import com.fundory.app.worker.ExchangeRateSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule periodic background exchange rate sync
        ExchangeRateSyncWorker.schedule(this)

        setContent {
            val preferences by userPreferencesRepository.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = null
            )

            // Keep splash screen on until preferences are loaded to prevent blank/white frames
            splashScreen.setKeepOnScreenCondition { preferences == null }

            val currentPrefs = preferences ?: return@setContent

            FundoryTheme(
                darkTheme = currentPrefs.isDarkMode,
                dynamicColor = currentPrefs.useDynamicColors
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startDestination = if (currentPrefs.hasCompletedOnboarding) {
                        Screen.Home.route
                    } else {
                        Screen.Onboarding.route
                    }

                    FundoryNavHost(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
