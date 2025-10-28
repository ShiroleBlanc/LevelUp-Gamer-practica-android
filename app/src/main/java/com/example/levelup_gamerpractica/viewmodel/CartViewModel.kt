package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.dao.CartItemWithDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Estado de la UI del Carrito
data class CartUiState(
    val items: List<CartItemWithDetails> = emptyList(),
    val totalAmount: Double = 0.0, // Cambiado a Double para cálculos más precisos
    val isLoading: Boolean = true
) {
    // Helper para formatear el total como CLP
    fun formattedTotal(): String {
        // Implementa lógica de formato CLP aquí si es necesario, o usa Double directamente
        return "$${totalAmount.toLong()}" // Formato simple por ahora
    }
}

class CartViewModel(private val repository: AppRepository) : ViewModel() {

    // Combina el Flow del carrito con el cálculo del total
    val uiState: StateFlow<CartUiState> = repository.cartItems
        .map { items ->
            println("CartViewModel: Mapeando items, tamaño = ${items.size}")
            val total = items.sumOf { item ->
                // Usa la función parsePrice importada o definida aquí
                parsePrice(item.price) * item.quantity.toDouble()
            }
            CartUiState(items = items, totalAmount = total, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CartUiState(isLoading = true)
        )

    fun increaseQuantity(productId: Int) {
        viewModelScope.launch { repository.increaseCartItemQuantity(productId) }
    }

    fun decreaseQuantity(productId: Int) {
        viewModelScope.launch { repository.decreaseCartItemQuantity(productId) }
    }

    fun removeFromCart(productId: Int) {
        viewModelScope.launch { repository.removeFromCart(productId) }
    }

    fun clearCart() {
        viewModelScope.launch { repository.clearCart() }
    }

    // --- Helper para parsear precio (igual que en el Context de React) ---
    private fun parsePrice(value: String): Double {
        if (!value.contains("$")) return 0.0 // Manejo básico si no tiene $
        val cleaned = value.replace(Regex("[^0-9]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}

// Factory
class CartViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}