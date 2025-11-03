package com.example.levelup_gamerpractica.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.levelup_gamerpractica.data.local.LevelUpGamerApplication
import com.example.levelup_gamerpractica.navigation.Routes
import com.example.levelup_gamerpractica.viewmodel.MainViewModel
import com.example.levelup_gamerpractica.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    navController: NavController,
    // ViewModel para saber el estado de autenticación
    mainViewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory((LocalContext.current.applicationContext as LevelUpGamerApplication).repository)
    ),
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Observamos el estado del usuario desde el ViewModel
    val currentUser by mainViewModel.currentUser.collectAsState()

    // Observamos la ruta actual para saber qué item del menú resaltar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))

                // --- Items SIEMPRE visibles ---
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Storefront, contentDescription = "Catálogo") },
                    label = { Text("Catálogo") },
                    selected = currentRoute == Routes.CATALOG,
                    onClick = {
                        navController.navigate(Routes.CATALOG)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrito") },
                    label = { Text("Carrito") },
                    selected = currentRoute == Routes.CART,
                    onClick = {
                        navController.navigate(Routes.CART)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Divider para separar las secciones
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // --- INICIO: LÓGICA CONDICIONAL DEL MENÚ ---
                if (currentUser == null) {
                    // --- MENÚ PARA USUARIOS NO LOGUEADOS ---
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Login, contentDescription = "Iniciar Sesión") },
                        label = { Text("Iniciar Sesión") },
                        selected = currentRoute == Routes.LOGIN,
                        onClick = {
                            navController.navigate(Routes.LOGIN)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.PersonAdd, contentDescription = "Registrarme") },
                        label = { Text("Registrarme") },
                        selected = currentRoute == Routes.REGISTER,
                        onClick = {
                            navController.navigate(Routes.REGISTER)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                } else {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Logout, contentDescription = "Cerrar Sesión") },
                        label = { Text("Cerrar Sesión") },
                        selected = false,
                        onClick = {
                            mainViewModel.logout()
                            scope.launch { drawerState.close() }
                            // Navegamos al Login AQUÍ, al hacer clic
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            } // <- AÑADIR ESTE PARÉNTESIS DE CIERRE
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(getTitleForRoute(currentRoute, currentUser?.username)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Abrir Menú")
                        }
                    }
                )
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}


// Función helper para obtener el título de la TopAppBar
fun getTitleForRoute(route: String?, userName: String?): String {
    return when (route) {
        Routes.CATALOG -> userName ?: "Catálogo"
        Routes.CART -> "Carrito"
        Routes.LOGIN -> "Iniciar Sesión"
        Routes.REGISTER -> "Registro"
        else -> "LevelUp Gamer"
    }
}