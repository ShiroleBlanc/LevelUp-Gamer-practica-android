package com.example.levelup_gamerpractica.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter 
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage 
import com.example.levelup_gamerpractica.data.local.LevelUpGamerApplication
import com.example.levelup_gamerpractica.navigation.Routes
import com.example.levelup_gamerpractica.viewmodel.MainViewModel
import com.example.levelup_gamerpractica.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import com.example.levelup_gamerpractica.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    navController: NavController,
    mainViewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory((LocalContext.current.applicationContext as LevelUpGamerApplication).repository)
    ),
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentUser by mainViewModel.currentUser.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val startDestinationId = navController.graph.startDestinationId

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Storefront, contentDescription = "Catálogo") },
                    label = { Text("Catálogo") },
                    selected = currentRoute == Routes.CATALOG,
                    onClick = {
                        navController.navigate(Routes.CATALOG) {
                            popUpTo(startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrito") },
                    label = { Text("Carrito") },
                    selected = currentRoute == Routes.CART,
                    onClick = {
                        navController.navigate(Routes.CART) {
                            popUpTo(startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (currentUser == null) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Login, contentDescription = "Iniciar Sesión") },
                        label = { Text("Iniciar Sesión") },
                        selected = currentRoute == Routes.LOGIN,
                        onClick = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.PersonAdd, contentDescription = "Registrarme") },
                        label = { Text("Registrarme") },
                        selected = currentRoute == Routes.REGISTER,
                        onClick = {
                            navController.navigate(Routes.REGISTER) {
                                popUpTo(startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                } else {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Mi Perfil") },
                        label = { Text("Mi Perfil") },
                        selected = currentRoute == Routes.PROFILE,
                        onClick = {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Logout, contentDescription = "Cerrar Sesión") },
                        label = { Text("Cerrar Sesión") },
                        selected = false,
                        onClick = {
                            mainViewModel.logout()
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(            topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getTitleForRoute(currentRoute, currentUser?.username),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // EL LOGO
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .height(40.dp)
                                .padding(end = 8.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                    }
                },
                    actions = {
                        if (currentUser != null) {
                            IconButton(
                                onClick = { navController.navigate(Routes.PROFILE) },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                val placeholderPainter = rememberVectorPainter(
                                    Icons.Filled.AccountCircle
                                )
                                AsyncImage(
                                    model = currentUser?.profilePictureUrl,
                                    contentDescription = "Foto de perfil",

                                    placeholder = placeholderPainter,
                                    error = placeholderPainter,
                                    fallback = placeholderPainter,

                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
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
        Routes.CATALOG -> "Catálogo"
        Routes.CART -> "Carrito"
        Routes.LOGIN -> "Iniciar Sesión"
        Routes.REGISTER -> "Registro"
        Routes.PROFILE -> "Mi Perfil" 
        else -> "LevelUp Gamer"
    }
}

