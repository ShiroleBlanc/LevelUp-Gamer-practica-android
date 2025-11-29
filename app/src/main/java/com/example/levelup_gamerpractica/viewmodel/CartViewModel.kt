package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.dao.CartItemWithDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

data class CartUiState(
    val items: List<CartItemWithDetails> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true
) {
    // Helper para mostrar el total bonito en la UI
    fun formattedTotal(): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
        format.maximumFractionDigits = 0
        return format.format(totalAmount)
    }
}

class CartViewModel(private val repository: AppRepository) : ViewModel() {

    val uiState: StateFlow<CartUiState> = repository.cartItems
        .map { items ->
            // --- CORRECCIÓN AQUÍ ---
            // 1. Accedemos a item.product.price (que ya es Double)
            // 2. Accedemos a item.cartItem.quantity
            // 3. Ya no usamos parsePrice porque el precio ya es numérico

            val total = items.sumOf { item ->
                item.product.price * item.cartItem.quantity
            }

            CartUiState(items = items, totalAmount = total, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CartUiState(isLoading = true)
        )

    fun increaseQuantity(productId: Long) {
        viewModelScope.launch { repository.increaseCartItemQuantity(productId) }
    }

    fun decreaseQuantity(productId: Long) {
        viewModelScope.launch { repository.decreaseCartItemQuantity(productId) }
    }

    fun removeFromCart(productId: Long) {
        viewModelScope.launch { repository.removeFromCart(productId) }
    }

    fun clearCart() {
        viewModelScope.launch { repository.clearCart() }
    }

    // He eliminado la función private fun parsePrice(...) porque ya no sirve.
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