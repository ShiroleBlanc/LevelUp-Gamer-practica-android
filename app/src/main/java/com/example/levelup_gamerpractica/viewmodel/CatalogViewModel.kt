package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.entities.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "Todos",
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(private val repository: AppRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("Todos")

    // Combina el Flow de categorías y el Flow de productos filtrados
    val uiState: StateFlow<CatalogUiState> = combine(
        repository.allCategories, // Flow de todas las categorías
        _selectedCategory.flatMapLatest { category -> // Flow que cambia según la categoría seleccionada
            if (category == "Todos") {
                repository.allProducts
            } else {
                repository.getProductsByCategory(category)
            }
        },
        _selectedCategory // Necesitamos el StateFlow de la categoría seleccionada aquí también
    ) { categories, products, selectedCat ->
        CatalogUiState(
            products = products,
            categories = listOf("Todos") + categories, // Añade "Todos" al principio
            selectedCategory = selectedCat,
            isLoading = false // O maneja un estado de carga si la carga inicial es lenta
        )
    }.stateIn( // Convierte el Flow combinado en un StateFlow
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Empieza a colectar cuando hay suscriptores
        initialValue = CatalogUiState() // Estado inicial mientras carga
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // Función para añadir al carrito (usando el repositorio)
    fun addToCart(product: Product) {
        viewModelScope.launch {
            repository.addToCart(product.id)
            // Podrías añadir un StateFlow para mostrar un Snackbar/Toast de confirmación
        }
    }
}

// Factory
class CatalogViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CatalogViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}