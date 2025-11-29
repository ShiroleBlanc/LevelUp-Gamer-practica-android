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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    // --- MANEJO DE RESPUESTAS DEL BACKEND ---

    // 1. Escuchar Éxito
    LaunchedEffect(uiState.checkoutSuccess) {
        if (uiState.checkoutSuccess != null) {
            val orderId = uiState.checkoutSuccess!!.id
            Toast.makeText(context, "¡Compra Exitosa! Orden #$orderId", Toast.LENGTH_LONG).show()
            cartViewModel.resetCheckoutStatus()
            // Aquí podrías navegar a la pantalla de "Mis Pedidos" si quisieras
        }
    }

    // 2. Escuchar Errores
    LaunchedEffect(uiState.checkoutError) {
        if (uiState.checkoutError != null) {
            Toast.makeText(context, "Error: ${uiState.checkoutError}", Toast.LENGTH_LONG).show()
            cartViewModel.resetCheckoutStatus()
        }
    }

    val formatClp = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply {
            maximumFractionDigits = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Si está cargando y NO es por checkout (ej. carga inicial), mostramos loading completo
        if (uiState.isLoading && uiState.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.items.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Tu carrito está vacío.", style = MaterialTheme.typography.headlineSmall)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.items, key = { item -> item.productId }) { item ->
                    CartItemRow(
                        item = item,
                        onIncrease = { cartViewModel.increaseQuantity(item.productId) },
                        onDecrease = { cartViewModel.decreaseQuantity(item.productId) },
                        onRemove = { cartViewModel.removeFromCart(item.productId) },
                        currencyFormatter = formatClp
                    )
                }
            }

            // --- Resumen y Botones ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Total: ${formatClp.format(uiState.totalAmount)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    OutlinedButton(
                        onClick = { showEmptyCartDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading // Deshabilitar si se está procesando
                    ) {
                        Icon(Icons.Filled.RemoveShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Vaciar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // --- BOTÓN FINALIZAR COMPRA ACTUALIZADO ---
                    Button(
                        onClick = {
                            // LLAMADA REAL AL BACKEND
                            cartViewModel.performCheckout()
                        },
                        modifier = Modifier.weight(1f),
                        // Deshabilitar botón mientras carga para evitar doble compra
                        enabled = uiState.items.isNotEmpty() && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            // Mostrar spinner dentro del botón mientras procesa
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null)
                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Finalizar")
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para vaciar carrito
    if (showEmptyCartDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyCartDialog = false },
            title = { Text("Confirmar") },
            text = { Text("¿Estás seguro que quieres vaciar el carrito?") },
            confirmButton = {
                Button(
                    onClick = {
                        cartViewModel.clearCart()
                        showEmptyCartDialog = false
                    }
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
    val context = LocalContext.current
    val imageResId = remember(item.image) {
        try {
            val imageName = item.image.substringAfterLast('/').substringBeforeLast('.')
            context.resources.getIdentifier(imageName, "drawable", context.packageName)
        }
        catch (e: Exception) { 0 }
    }

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(end = 12.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(60.dp).padding(end=12.dp)) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage,
                        contentDescription = "No image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(currencyFormatter.format(parsePrice(item.price)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(36.dp), enabled = item.quantity > 1) {
                    Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Disminuir")
                }
                Text(
                    "${item.quantity}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onIncrease, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = "Aumentar")
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun parsePrice(value: String): Double {
    val cleaned = value.replace(Regex("[^0-9]"), "")
    return cleaned.toDoubleOrNull() ?: 0.0
}