package com.example.levelup_gamerpractica.navigation

import androidx.compose.runtime.Composable
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
    // Añadiremos más si la app crece (ej. Product Detail)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // Navega al catálogo y limpia la pila de atrás para no volver al login
                    navController.navigate(Routes.CATALOG) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    // Vuelve a la pantalla de login después del registro
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true } // Vuelve al login limpiando registro
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // Rutas que estarán dentro del Scaffold con el Drawer
        composable(Routes.CATALOG) {
            MainAppScaffold(navController = navController) { // Pasa el navController al Scaffold
                CatalogScreen(
                    // onProductClick = { productId -> navController.navigate("productDetail/$productId") } // Ejemplo para detalle
                )
            }
        }
        composable(Routes.CART) {
            MainAppScaffold(navController = navController) {
                CartScreen()
            }
        }
        // Puedes añadir más composables aquí para otras pantallas dentro del Drawer
    }
}