package com.example.levelup_gamerpractica.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.levelup_gamerpractica.data.local.LevelUpGamerApplication
import com.example.levelup_gamerpractica.data.local.dao.CartItemWithDetails
import com.example.levelup_gamerpractica.viewmodel.CartViewModel
import com.example.levelup_gamerpractica.viewmodel.CartViewModelFactory
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CartScreen(
    paddingValues: PaddingValues,
    cartViewModel: CartViewModel = viewModel(
        factory = CartViewModelFactory((LocalContext.current.applicationContext as LevelUpGamerApplication).repository)
    )
) {
    val uiState by cartViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showEmptyCartDialog by remember { mutableStateOf(false) }

    // Formateador de moneda (CLP)
    val formatClp = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply {
            maximumFractionDigits = 0
        }
    }

    // --- EFECTO PARA MENSAJES DE CHECKOUT ---
    LaunchedEffect(uiState.checkoutMessage) {
        uiState.checkoutMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            cartViewModel.consumeCheckoutMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tu carrito está vacío.", style = MaterialTheme.typography.headlineSmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.items, key = { item -> item.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        onIncrease = { cartViewModel.increaseQuantity(item.product.id) },
                        onDecrease = { cartViewModel.decreaseQuantity(item.product.id) },
                        onRemove = { cartViewModel.removeFromCart(item.product.id) },
                        currencyFormatter = formatClp
                    )
                }
            }

            // --- Resumen y Botones ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total a Pagar:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = formatClp.format(uiState.totalAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showEmptyCartDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Vaciar")
                        }

                        // --- BOTÓN PAGAR REAL ---
                        Button(
                            onClick = {
                                // Llamada al checkout real
                                cartViewModel.performCheckout()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Pagar")
                        }
                    }
                }
            }
        }
    }

    if (showEmptyCartDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyCartDialog = false },
            title = { Text("Vaciar Carrito") },
            text = { Text("¿Estás seguro que deseas eliminar todos los productos?") },
            confirmButton = {
                Button(
                    onClick = {
                        cartViewModel.clearCart()
                        showEmptyCartDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Vaciar") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyCartDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CartItemRow(
    item: CartItemWithDetails,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    currencyFormatter: NumberFormat
) {
    val product = item.product
    val quantity = item.cartItem.quantity

    val isUrl = product.imageUrl.startsWith("http")
    val context = LocalContext.current

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .padding(end = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUrl) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val resId = try {
                        context.resources.getIdentifier(product.imageUrl.substringAfterLast("/").substringBefore("."), "drawable", context.packageName)
                    } catch (e: Exception) { 0 }

                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Filled.ImageNotSupported, contentDescription = null)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currencyFormatter.format(product.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                FilledIconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Disminuir", tint = MaterialTheme.colorScheme.onSurface)
                }

                Text(
                    text = "$quantity",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                FilledIconButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Aumentar")
                }
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}