package app.auriel.basalt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.auriel.basalt.core.data.rememberGraph
import app.auriel.basalt.core.design.BasaltThemeHost
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.feature.alarm.AlarmScreen
import app.auriel.basalt.feature.alarm.RingtonePickerScreen
import app.auriel.basalt.feature.bedtime.BedtimeScreen
import app.auriel.basalt.feature.clock.ClockScreen
import app.auriel.basalt.feature.settings.SettingsScreen
import app.auriel.basalt.feature.stopwatch.StopwatchScreen
import app.auriel.basalt.feature.timer.TimerScreen

/**
 * Root of the app. Built on foundation + navigation only: there is no
 * Scaffold here because there is no Material here.
 */
@Composable
fun BasaltApp(startRoute: String? = null) {
    val graph = rememberGraph()
    BasaltThemeHost(
        themeIdFlow = graph.themeIds,
        initialThemeId = graph.cachedThemeId,
        styleIdFlow = graph.uiStyleIds,
        initialStyleId = graph.cachedUiStyleId,
    ) {
        val colors = LocalBasaltColors.current
        // A widget names the section it is a widget for; anything else — an
        // unknown route, a launcher tap — opens where the app always opens.
        val openingRoute = remember(startRoute) {
            BasaltDestination.entries.firstOrNull { it.route == startRoute }?.route
                ?: BasaltDestination.Start.route
        }
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
            ?: openingRoute

        /**
         * Switches top-level section.
         *
         * The current route is read from the controller at click time, not
         * captured from composition. It has to be: `onSelect = ::go` passes a
         * *function reference*, and Compose compares those as equal across
         * recompositions, so the tab strip holds on to the first one it was
         * given — along with whatever `currentRoute` was when that composition
         * ran. Capturing the route made every tap think it was already on
         * Clock, and tapping CLOCK from another tab did nothing at all.
         *
         * The back stack is kept flat and predictable: either `[Clock]` or
         * `[Clock, section]`, never deeper. Reaching Clock is therefore a pop
         * rather than a navigate, which also sidesteps the
         * `popUpTo(start) { saveState = true }` + `launchSingleTop` recipe
         * treating a navigation to the start destination as already satisfied.
         */
        fun go(destination: BasaltDestination) {
            val current = navController.currentDestination?.route
            if (destination.route == current) return
            // The root is whatever the app opened on, which a widget tap can
            // change. Popping to a route that is not on the stack does
            // nothing at all, so this has to follow the start destination
            // rather than assume Clock.
            if (destination.route == openingRoute) {
                navController.popBackStack(openingRoute, inclusive = false)
            } else {
                navController.navigate(destination.route) {
                    popUpTo(openingRoute) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ink)
                .systemBarsPadding(),
        ) {
            BasaltTopBar(
                onOpenSettings = { go(BasaltDestination.Settings) },
                settingsSelected = currentRoute == BasaltDestination.Settings.route,
            )

            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = openingRoute,
                ) {
                    composable(BasaltDestination.Alarm.route) {
                        AlarmScreen(
                            onEditSound = { alarmId ->
                                navController.navigate("ringtone/$alarmId")
                            },
                        )
                    }

                    // Not a tab: reached from an alarm, and returns to it.
                    composable(
                        route = "ringtone/{alarmId}",
                        arguments = listOf(navArgument("alarmId") { type = NavType.LongType }),
                    ) { entry ->
                        RingtonePickerScreen(
                            alarmId = entry.arguments?.getLong("alarmId") ?: 0L,
                            onDone = { navController.popBackStack() },
                        )
                    }
                    composable(BasaltDestination.Timer.route) { TimerScreen() }
                    composable(BasaltDestination.Clock.route) { ClockScreen() }
                    composable(BasaltDestination.Stopwatch.route) { StopwatchScreen() }
                    composable(BasaltDestination.Bedtime.route) { BedtimeScreen() }
                    composable(BasaltDestination.Settings.route) { SettingsScreen() }
                }
            }

            BasaltTabBar(
                current = currentRoute,
                onSelect = ::go,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}
