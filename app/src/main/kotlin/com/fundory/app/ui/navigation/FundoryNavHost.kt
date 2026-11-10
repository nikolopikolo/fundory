package com.fundory.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.fundory.app.ui.analytics.AnalyticsScreen
import com.fundory.app.ui.dashboard.DashboardScreen
import com.fundory.app.ui.goals.AddEditGoalScreen
import com.fundory.app.ui.goals.AddEditProductScreen
import com.fundory.app.ui.goals.GoalDetailScreen
import com.fundory.app.ui.goals.GoalsScreen
import com.fundory.app.ui.goals.ShippingCalculatorScreen
import com.fundory.app.ui.onboarding.OnboardingScreen
import com.fundory.app.ui.settings.SettingsScreen
import com.fundory.app.ui.transactions.TransactionsScreen

@Composable
fun FundoryNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Display bottom bar only on top-level tabs
    val showBottomBar = currentRoute in BottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FundoryBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                DashboardScreen(
                    onNavigateToGoalDetail = { goalId ->
                        navController.navigate(Screen.GoalDetail.createRoute(goalId))
                    },
                    onNavigateToAddGoal = {
                        navController.navigate(Screen.AddEditGoal.createRoute())
                    },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.createRoute())
                    },
                    onNavigateToAnalytics = {
                        navController.navigate(Screen.Analytics.route)
                    }
                )
            }

            composable(Screen.Goals.route) {
                GoalsScreen(
                    onNavigateToGoalDetail = { goalId ->
                        navController.navigate(Screen.GoalDetail.createRoute(goalId))
                    },
                    onNavigateToAddGoal = {
                        navController.navigate(Screen.AddEditGoal.createRoute())
                    },
                    onNavigateToEditGoal = { goalId ->
                        navController.navigate(Screen.AddEditGoal.createRoute(goalId))
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.GoalDetail.route,
                arguments = listOf(navArgument("goalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getString("goalId") ?: ""
                GoalDetailScreen(
                    goalId = goalId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditGoal = { id ->
                        navController.navigate(Screen.AddEditGoal.createRoute(id))
                    },
                    onNavigateToAddProduct = { id ->
                        navController.navigate(Screen.AddEditProduct.createRoute(id))
                    },
                    onNavigateToEditProduct = { gId, pId ->
                        navController.navigate(Screen.AddEditProduct.createRoute(gId, pId))
                    },
                    onNavigateToShippingCalculator = { id ->
                        navController.navigate(Screen.ShippingCalculator.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.AddEditGoal.route,
                arguments = listOf(
                    navArgument("goalId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getString("goalId")
                AddEditGoalScreen(
                    goalId = goalId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AddEditProduct.route,
                arguments = listOf(
                    navArgument("goalId") { type = NavType.StringType },
                    navArgument("productId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getString("goalId") ?: ""
                val productId = backStackEntry.arguments?.getString("productId")
                AddEditProductScreen(
                    goalId = goalId,
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ShippingCalculator.route,
                arguments = listOf(
                    navArgument("goalId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getString("goalId")
                ShippingCalculatorScreen(
                    goalId = goalId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Transactions.route,
                arguments = listOf(
                    navArgument("goalId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                TransactionsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
