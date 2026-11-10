package com.fundory.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.fundory.app.data.preferences.UserPreferencesRepository
import com.fundory.app.data.repository.GoalRepository
import com.fundory.app.data.repository.ProductRepository
import com.fundory.app.domain.engine.ShareLinkParser
import com.fundory.app.domain.engine.SharedProductInfo
import com.fundory.app.ui.components.ShareImportBottomSheet
import com.fundory.app.ui.navigation.FundoryNavHost
import com.fundory.app.ui.navigation.Screen
import com.fundory.app.ui.theme.FundoryTheme
import com.fundory.app.worker.ExchangeRateSyncWorker
import androidx.compose.material3.ExperimentalMaterial3Api
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var productRepository: ProductRepository

    @Inject
    lateinit var goalRepository: GoalRepository

    private var pendingSharedInfo by mutableStateOf<SharedProductInfo?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        // Schedule periodic background exchange rate sync
        ExchangeRateSyncWorker.schedule(this)

        setContent {
            val preferences by userPreferencesRepository.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = null
            )
            val activeGoals by goalRepository.activeGoalsWithProductsFlow.collectAsStateWithLifecycle(
                initialValue = emptyList()
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

                    // Native Share Import Bottom Sheet
                    pendingSharedInfo?.let { shared ->
                        ShareImportBottomSheet(
                            sharedInfo = shared,
                            activeGoals = activeGoals,
                            onDismiss = { pendingSharedInfo = null },
                            onSaveProduct = { product ->
                                lifecycleScope.launch {
                                    productRepository.saveProduct(product)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val parsed = ShareLinkParser.parse(sharedText)
            if (parsed != null) {
                pendingSharedInfo = parsed
            }
        }
    }
}
