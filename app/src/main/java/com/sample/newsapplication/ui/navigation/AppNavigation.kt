package com.sample.newsapplication.ui.navigation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sample.newsapplication.ui.screens.home.HomeScreen
import com.sample.newsapplication.ui.screens.search.SearchScreen
import com.sample.newsapplication.ui.screens.search.SearchScreenRoute
import com.sample.newsapplication.utilities.Routes


@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    Surface(modifier = modifier) {
        val navController = rememberNavController();
        NavHost(navController = navController, startDestination = Routes.HOME_SCREEN) {
            composable(Routes.HOME_SCREEN) {
                HomeScreen(navigator = navController)
            }

            composable(Routes.SEARCH_SCREEN) {
                SearchScreenRoute(navigator = navController)
            }
        }
    }
}