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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBar
@Composable
fun CartScreen(
    cartViewModel: CartViewModel = viewModel(
        factory = CartViewModelFactory((LocalContext.current.applicationContext as LevelUpGamerApplication).repository)
    )
) {
    val uiState by cartViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showEmptyCartDialog by remember { mutableStateOf(false) }

    val formatClp = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply {
            maximumFractionDigits = 0
        }
    }

    // 1. Usamos Scaffold como contenedor principal
    Scaffold(
        topBar = {
            // 2. Aquí va tu barra de navegación o header.
            // Si no tienes uno, puedes usar un TopAppBar de ejemplo.
            TopAppBar(
                title = { Text("Mi Carrito") }
            )
        }
    ) { innerPadding -> // 3. Scaffold nos da un padding para evitar la superposición

        // 4. Aplicamos ese padding al contenedor de nuestro contenido
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Tu carrito está vacío.", style = MaterialTheme.typography.headlineSmall)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f), // Ocupa el espacio disponible
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), // Ajusta el padding si es necesario
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
                    // ... (El resto de tu código para el total y los botones se mantiene igual)
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
                            enabled = uiState.items.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.RemoveShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Vaciar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, "Compra finalizada (simulado)", Toast.LENGTH_SHORT).show()
                                cartViewModel.clearCart()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.items.isNotEmpty()
                        ) {
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

// Composable para una fila del carrito
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
        try { context.resources.getIdentifier(item.image.substringAfterLast('/'), "drawable", context.packageName) }
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
                Spacer(modifier = Modifier.size(60.dp).padding(end=12.dp)) // Placeholder
            }


            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(currencyFormatter.format(parsePrice(item.price)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }

            // Controles de cantidad
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(36.dp)) {
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

            // Botón Eliminar
            IconButton(onClick = onRemove, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// Helper para parsear precio (debe ser consistente con el ViewModel)
private fun parsePrice(value: String): Double {
    if (!value.contains("$")) return 0.0
    val cleaned = value.replace(Regex("[^0-9]"), "")
    return cleaned.toDoubleOrNull() ?: 0.0
}