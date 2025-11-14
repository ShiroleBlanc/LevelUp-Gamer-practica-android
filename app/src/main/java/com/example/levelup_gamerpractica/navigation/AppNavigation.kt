package com.example.levelup_gamerpractica.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.levelup_gamerpractica.ui.screens.CartScreen
import com.example.levelup_gamerpractica.ui.screens.CatalogScreen
import com.example.levelup_gamerpractica.ui.screens.LoginScreen
import com.example.levelup_gamerpractica.ui.screens.ProfileScreen
import com.example.levelup_gamerpractica.ui.screens.RegisterScreen
import com.example.levelup_gamerpractica.ui.screens.MainAppScaffold

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CATALOG = "catalog"
    const val CART = "cart"
    const val PROFILE = "profile" 
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CATALOG) {
        composable(Routes.LOGIN) {
            MainAppScaffold(navController = navController) { innerPadding ->
                LoginScreen(
                    modifier = Modifier.padding(innerPadding),
                    onLoginSuccess = {
                        navController.navigate(Routes.CATALOG) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
        }
        composable(Routes.REGISTER) {
            MainAppScaffold(navController = navController) { innerPadding ->
                RegisterScreen(
                    modifier = Modifier.padding(innerPadding),
                    onRegisterSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Routes.CATALOG) {
            MainAppScaffold(navController = navController) { innerPadding ->
                CatalogScreen(
                    paddingValues = innerPadding
                )
            }
        }
        composable(Routes.CART) {
            MainAppScaffold(navController = navController) { innerPadding ->
                CartScreen(
                    paddingValues = innerPadding
                )
            }
        }
        composable(Routes.PROFILE) {
            MainAppScaffold(navController = navController) { innerPadding ->
                ProfileScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
