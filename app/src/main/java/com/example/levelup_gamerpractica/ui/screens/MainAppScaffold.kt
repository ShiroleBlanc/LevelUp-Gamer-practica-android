package com.example.levelup_gamerpractica.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))

                if (currentUser != null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Hola, ${currentUser?.username}", style = MaterialTheme.typography.titleLarge)
                    }
                    HorizontalDivider()
                }
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Storefront, contentDescription = "Catálogo") },
                    label = { Text("Catálogo") },
                    selected = currentRoute == Routes.CATALOG,
                    onClick = {
                        navController.navigate(Routes.CATALOG) {
                            popUpTo(Routes.CATALOG) {
                                inclusive = false
                            }
                            launchSingleTop = true
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
                            popUpTo(Routes.CATALOG) {
                                saveState = true
                            }
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
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Mi Perfil") },
                        label = { Text("Mi Perfil") },
                        selected = currentRoute == Routes.PROFILE,
                        onClick = {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(Routes.CATALOG) {
                                    saveState = true
                                }
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
                            // Ir al Login y borrar TODO el historial anterior
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = getTitleForRoute(currentRoute, currentUser?.username),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1
                            )
                            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier.height(32.dp).padding(start = 8.dp)
                                )
                            }
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
                                if (currentUser?.profilePictureUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(currentUser?.profilePictureUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Foto de perfil",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Filled.AccountCircle, "Perfil", modifier = Modifier.size(32.dp))
                                }
                            }
                        } else {
                            IconButton(onClick = { navController.navigate(Routes.LOGIN) }) {
                                Icon(Icons.Filled.Login, "Login")
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