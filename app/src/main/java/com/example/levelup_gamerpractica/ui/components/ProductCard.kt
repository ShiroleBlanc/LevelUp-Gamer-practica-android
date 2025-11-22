package com.example.levelup_gamerpractica.ui.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.levelup_gamerpractica.data.local.entities.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    onProductClick: (Int) -> Unit,
    onAddToCartClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val iconScale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        label = "Icon Scale Animation"
    )

    // Lógica para determinar si es URL o Recurso Local
    val isUrl = product.image.startsWith("http")

    // Si no es URL, intentamos buscar el ID del drawable por su nombre
    val drawableResId = remember(product.image) {
        if (!isUrl) {
            // Limpiamos el nombre (por si viene con extensión o ruta)
            // Ej: "/assets/polera.png" -> "polera"
            val cleanName = product.image.substringAfterLast("/").substringBeforeLast(".")
            context.resources.getIdentifier(cleanName, "drawable", context.packageName)
        } else {
            0
        }
    }

    Card(
        modifier = modifier.clickable { onProductClick(product.id.toInt()) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {

            // --- RENDERIZADO DE IMAGEN HÍBRIDO ---
            if (isUrl) {
                // Opción A: Cargar desde Internet (Coil)
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.image)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                ) {
                    val state = painter.state
                    if (state is AsyncImagePainter.State.Loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (state is AsyncImagePainter.State.Error) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = "Error de imagen",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
            } else {
                // Opción B: Cargar desde Drawable (Local)
                if (drawableResId != 0) {
                    Image(
                        painter = painterResource(id = drawableResId),
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop // Ajuste para que llene el cuadro
                    )
                } else {
                    // Opción C: No se encontró ni URL ni Drawable válido
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ImageNotSupported,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Imagen no disponible", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
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
                    IconButton(
                        onClick = { onAddToCartClick(product) },
                        modifier = Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitFirstDown(requireUnconsumed = false)
                                    isPressed = true
                                    waitForUpOrCancellation()
                                    isPressed = false
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddShoppingCart,
                            contentDescription = "Añadir al carrito",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.scale(iconScale)
                        )
                    }
                }
            }
        }
    }
}