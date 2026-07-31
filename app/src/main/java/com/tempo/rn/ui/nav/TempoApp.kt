package com.tempo.rn.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tempo.rn.feature.forecast.ForecastScreen
import com.tempo.rn.feature.home.HomeScreen
import com.tempo.rn.feature.map.MapScreen
import com.tempo.rn.feature.models.ModelsScreen
import com.tempo.rn.feature.tide.TideScreen
import com.tempo.rn.ui.icons.TempoIcons

private data class NavItem(
    val route: String,
    val labelPt: String,
    val icon: ImageVector,
)

private val navItems = listOf(
    NavItem("home", "Início", TempoIcons.Home),
    NavItem("forecast", "Previsão", TempoIcons.Forecast),
    NavItem("map", "Mapas", TempoIcons.Map),
    NavItem("tide", "Maré", TempoIcons.Wave),
    NavItem("models", "Modelos", TempoIcons.Layers),
)

@Composable
fun TempoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.labelPt) },
                        label = { Text(item.labelPt, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") { HomeScreen() }
            composable("forecast") { ForecastScreen() }
            composable("map") { MapScreen() }
            composable("tide") { TideScreen() }
            composable("models") { ModelsScreen() }
        }
    }
}
