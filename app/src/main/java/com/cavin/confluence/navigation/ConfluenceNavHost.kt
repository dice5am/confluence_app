package com.cavin.confluence.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.cavin.confluence.feature.alerts.AlertsRoute
import com.cavin.confluence.feature.chart.ChartRoute
import com.cavin.confluence.feature.home.HomeRoute

private data class TopLevel(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val topLevel = listOf(
    TopLevel(Routes.HOME, "Home", Icons.Outlined.Home),
    TopLevel(Routes.CHART, "Chart", Icons.Outlined.ShowChart),
    TopLevel(Routes.ALERTS, "Alerts", Icons.Outlined.Notifications),
)

@Composable
fun ConfluenceNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevel.forEach { dest ->
                    val selected = when (dest.route) {
                        Routes.CHART -> currentRoute?.startsWith("chart") == true
                        else -> currentRoute == dest.route
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            val target = if (dest.route == Routes.CHART) {
                                Routes.chart(tf = "1h")
                            } else {
                                dest.route
                            }
                            navController.navigate(target) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
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
                SettingsPlaceholder()
            }
        }
    }
}

@Composable
private fun SettingsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Settings — placeholder",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
