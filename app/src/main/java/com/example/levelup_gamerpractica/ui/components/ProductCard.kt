package com.example.levelup_gamerpractica.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.levelup_gamerpractica.data.local.entities.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    onProductClick: (Int) -> Unit,
    onAddToCartClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Intenta obtener el ID del recurso drawable a partir del nombre guardado
    val imageResId = remember(product.image) {
        try {
            context.resources.getIdentifier(product.image.substringAfterLast('/'), "drawable", context.packageName)
        } catch (e: Exception){
            0
        }
    }


    Card(
        modifier = modifier.clickable { onProductClick(product.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f), // Imagen cuadrada
                    contentScale = ContentScale.Crop
                )
            } else {
                // Espacio reservado si no hay imagen o hay error
                Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp))
                Text("Imagen no disponible", style = MaterialTheme.typography.bodySmall)
            }


            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = product.price,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    IconButton(onClick = { onAddToCartClick(product) }) {
                        Icon(
                            Icons.Filled.AddShoppingCart,
                            contentDescription = "Añadir al carrito",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}