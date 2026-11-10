package com.fundory.app.ui.navigation

sealed class Screen(val route: String) {
    // Bottom Nav Tabs
    data object Home : Screen("home")
    data object Goals : Screen("goals")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")

    // Sub screens
    data object GoalDetail : Screen("goal_detail/{goalId}") {
        fun createRoute(goalId: String) = "goal_detail/$goalId"
    }

    data object AddEditGoal : Screen("add_edit_goal?goalId={goalId}") {
        fun createRoute(goalId: String? = null) = if (goalId != null) "add_edit_goal?goalId=$goalId" else "add_edit_goal"
    }

    data object AddEditProduct : Screen("add_edit_product/{goalId}?productId={productId}") {
        fun createRoute(goalId: String, productId: String? = null) =
            if (productId != null) "add_edit_product/$goalId?productId=$productId" else "add_edit_product/$goalId"
    }

    data object ShippingCalculator : Screen("shipping_calculator?goalId={goalId}") {
        fun createRoute(goalId: String? = null) =
            if (goalId != null) "shipping_calculator?goalId=$goalId" else "shipping_calculator"
    }

    data object Transactions : Screen("transactions?goalId={goalId}") {
        fun createRoute(goalId: String? = null) =
            if (goalId != null) "transactions?goalId=$goalId" else "transactions"
    }

    data object Wishlist : Screen("wishlist")

    data object Onboarding : Screen("onboarding")
}

