package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.dao.CartItemWithDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartUiState(
    val items: List<CartItemWithDetails> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true
) {
    fun formattedTotal(): String {
        return "$${totalAmount.toLong()}"
    }
}

class CartViewModel(private val repository: AppRepository) : ViewModel() {

    val uiState: StateFlow<CartUiState> = repository.cartItems
        .map { items ->
            println("CartViewModel: Mapeando items, tamaño = ${items.size}")
            val total = items.sumOf { item ->
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

    private fun parsePrice(value: String): Double {
        if (!value.contains("$")) return 0.0
        val cleaned = value.replace(Regex("[^0-9]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}

class CartViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}