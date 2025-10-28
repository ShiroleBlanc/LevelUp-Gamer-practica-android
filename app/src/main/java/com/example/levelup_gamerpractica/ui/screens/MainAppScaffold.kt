package com.example.levelup_gamerpractica.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.levelup_gamerpractica.navigation.Routes
import kotlinx.coroutines.launch

// Scaffold principal que incluye el Drawer y la TopAppBar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Obtiene la ruta actual para saber qué título mostrar y qué item resaltar
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                // Elementos del Drawer
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.ListAlt, contentDescription = "Catálogo") },
                    label = { Text("Catálogo") },
                    selected = currentRoute == Routes.CATALOG,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentRoute != Routes.CATALOG) {
                            navController.navigate(Routes.CATALOG) { popUpTo(Routes.CATALOG) {inclusive = true} }
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrito") },
                    label = { Text("Carrito") },
                    selected = currentRoute == Routes.CART,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentRoute != Routes.CART) {
                            navController.navigate(Routes.CART) { popUpTo(Routes.CATALOG) } // Vuelve al catálogo al salir
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                // Divider
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                // Perfil
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Perfil") },
                    label = { Text("Mi Perfil") },
                    selected = false, // O si tienes ruta de perfil: currentRoute == Routes.PROFILE
                    onClick = {
                        scope.launch { drawerState.close() }
                        // navController.navigate(Routes.PROFILE) // Si creas la ruta
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                // Logout
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Logout, contentDescription = "Cerrar Sesión") },
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // Limpia la pila y vuelve al login
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true } // Limpia toda la pila
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(getTitleForRoute(currentRoute)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                        }
                    }
                )
            }
        ) { innerPadding ->
            // El contenido de la pantalla actual (CatalogScreen, CartScreen, etc.)
            content(innerPadding)
        }
    }
}

// Función helper para obtener el título de la TopAppBar
fun getTitleForRoute(route: String?): String {
    return when (route) {
        Routes.CATALOG -> "Catálogo"
        Routes.CART -> "Carrito"
        // Añade más casos si tienes más pantallas
        else -> "LevelUp Gamer"
    }
}