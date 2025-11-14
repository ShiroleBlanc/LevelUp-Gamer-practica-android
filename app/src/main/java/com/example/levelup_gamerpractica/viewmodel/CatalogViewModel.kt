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
    val isLoading: Boolean = true,
    val userName: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(private val repository: AppRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("Todos")

    init {
        // Cuando el ViewModel se crea, pide al repositorio que refresque
        // los productos.
        viewModelScope.launch {
            repository.refreshProducts()
        }
    }

    val uiState: StateFlow<CatalogUiState> = combine(
        repository.allCategories, // Flow de todas las categorías
        _selectedCategory.flatMapLatest { category -> // Flow que cambia según la categoría
            if (category == "Todos") {
                repository.allProducts
            } else {
                repository.getProductsByCategory(category)
            }
        },
        _selectedCategory,
        repository.currentUserNameFlow
    ) { categories, products, selectedCat, userName ->
        CatalogUiState(
            products = products,
            categories = listOf("Todos") + categories,
            selectedCategory = selectedCat,
            userName = userName,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CatalogUiState()
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // Función para añadir al carrito (usando el repositorio)
    fun addToCart(product: Product) {
        viewModelScope.launch {
            repository.addToCart(product.id)
        }
    }
}

// Factory
// --- Factory para crear instancias de CatalogViewModel ---
class CatalogViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CatalogViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
