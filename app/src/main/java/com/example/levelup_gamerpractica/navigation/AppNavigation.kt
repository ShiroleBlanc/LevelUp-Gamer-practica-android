package com.example.levelup_gamerpractica.navigation

import androidx.compose.foundation.layout.padding // <-- 1. IMPORT AÑADIDO
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.levelup_gamerpractica.ui.screens.CartScreen
import com.example.levelup_gamerpractica.ui.screens.CatalogScreen
import com.example.levelup_gamerpractica.ui.screens.LoginScreen
import com.example.levelup_gamerpractica.ui.screens.RegisterScreen
import com.example.levelup_gamerpractica.ui.screens.MainAppScaffold

// Define las rutas como constantes
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CATALOG = "catalog"
    const val CART = "cart"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CATALOG) {

        // --- Rutas que AHORA SÍ tienen el Scaffold ---
        composable(Routes.LOGIN) {
            MainAppScaffold(navController = navController) { innerPadding ->
                LoginScreen(
                    modifier = Modifier.padding(innerPadding), // Pasa el padding
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
                    modifier = Modifier.padding(innerPadding), // Pasa el padding
                    onRegisterSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // --- Rutas que YA tenían el Scaffold ---
        composable(Routes.CATALOG) {
            MainAppScaffold(navController = navController) { innerPadding ->
                CatalogScreen(
                    paddingValues = innerPadding // Pasa el padding
                    // onProductClick = { ... }
                )
            }
        }
        composable(Routes.CART) {
            MainAppScaffold(navController = navController) { innerPadding ->
                CartScreen(
                    paddingValues = innerPadding // 2. ESTO AHORA FUNCIONARÁ
                )
            }
        }
    }
}