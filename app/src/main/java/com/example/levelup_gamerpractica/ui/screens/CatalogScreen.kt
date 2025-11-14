package com.example.levelup_gamerpractica.ui.screens


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.levelup_gamerpractica.data.local.LevelUpGamerApplication
import com.example.levelup_gamerpractica.ui.components.ProductCard
import com.example.levelup_gamerpractica.viewmodel.CatalogViewModel
import com.example.levelup_gamerpractica.viewmodel.CatalogViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    paddingValues: PaddingValues,
    catalogViewModel: CatalogViewModel = viewModel(
        factory = CatalogViewModelFactory((LocalContext.current.applicationContext as LevelUpGamerApplication).repository)
    )
) {
    val uiState by catalogViewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)) {

        // --- INICIO: MENSAJE DE BIENVENIDA ---
        if (uiState.userName != null) {
            Text(
                text = "¡Bienvenido, ${uiState.userName} A LevelUpGamer La mejor tienda gamer del mundo ",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }


        // --- Barra de Filtro de Categorías ---
        FilterBar(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = catalogViewModel::selectCategory
        )

        // --- Grid de Productos ---
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No hay productos en esta categoría.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.products, key = { product -> product.id }) { product ->
                    ProductCard(
                        product = product,
                        onProductClick = { /* TODO: Navegar a detalle */ },
                        onAddToCartClick = {
                            catalogViewModel.addToCart(it)
                            Toast.makeText(context, "${it.name} añadido al carrito", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

// Composable para la barra de filtros
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text("Filtrar por Categoría") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}