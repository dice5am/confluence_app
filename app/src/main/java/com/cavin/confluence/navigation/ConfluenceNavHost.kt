package com.cavin.confluence.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.cavin.confluence.core.ui.components.FloatingDock
import com.cavin.confluence.core.ui.components.FloatingDockItem
import com.cavin.confluence.core.ui.components.GlassCard
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.feature.alerts.AlertsRoute
import com.cavin.confluence.feature.chart.ChartRoute
import com.cavin.confluence.feature.home.HomeRoute

@Composable
fun ConfluenceNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedId = when {
        currentRoute?.startsWith("chart") == true -> "chart"
        currentRoute == Routes.ALERTS -> "alerts"
        else -> "home"
    }

    val dockItems = listOf(
        FloatingDockItem("home", "Home", Icons.Outlined.Home),
        FloatingDockItem("chart", "Chart", Icons.Outlined.ShowChart),
        FloatingDockItem("alerts", "Alerts", Icons.Outlined.Notifications),
    )

    Scaffold(
        containerColor = ConfluenceColors.Void,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            FloatingDock(
                items = dockItems,
                selectedId = selectedId,
                onSelect = { id ->
                    val target = when (id) {
                        "chart" -> Routes.chart(tf = "1h")
                        "alerts" -> Routes.ALERTS
                        else -> Routes.HOME
                    }
                    navController.navigate(target) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(
                route = Routes.HOME,
                deepLinks = listOf(
                    navDeepLink { uriPattern = Routes.deepLinkUri("home") },
                ),
            ) {
                HomeRoute(
                    onOpenChart = {
                        navController.navigate(Routes.chart(tf = "1h")) {
                            launchSingleTop = true
                        }
                    },
                    onOpenAlerts = {
                        navController.navigate(Routes.ALERTS) {
                            launchSingleTop = true
                        }
                    },
                    onOpenSettings = {
                        navController.navigate(Routes.SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                route = Routes.CHART_ROUTE_PATTERN,
                arguments = listOf(
                    navArgument(Routes.CHART_TF_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(Routes.CHART_ALERT_ID_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern =
                            "${Routes.DEEP_LINK_SCHEME}://${Routes.DEEP_LINK_HOST}/chart" +
                            "?tf={tf}&alertId={alertId}"
                    },
                    navDeepLink {
                        uriPattern =
                            "${Routes.DEEP_LINK_SCHEME}://${Routes.DEEP_LINK_HOST}/chart"
                    },
                ),
            ) { entry ->
                val tf = entry.arguments?.getString(Routes.CHART_TF_ARG)?.ifBlank { null }
                val alertId =
                    entry.arguments?.getString(Routes.CHART_ALERT_ID_ARG)?.ifBlank { null }
                ChartRoute(
                    timeframe = tf,
                    alertId = alertId,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.ALERTS,
                deepLinks = listOf(
                    navDeepLink { uriPattern = Routes.deepLinkUri("alerts") },
                ),
            ) {
                AlertsRoute(
                    onOpenAlert = { id ->
                        navController.navigate(Routes.chart(alertId = id)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(route = Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    val spacing = ConfluenceThemeAccess.spacing
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        GlassCard {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = ConfluenceColors.TextPrimary,
            )
            Text(
                text = "Preferences will live here.",
                style = MaterialTheme.typography.bodyMedium,
                color = ConfluenceColors.Slate,
            )
        }
    }
}
