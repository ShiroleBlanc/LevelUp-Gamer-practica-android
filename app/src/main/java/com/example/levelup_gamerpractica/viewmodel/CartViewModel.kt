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

// Añadimos estados para manejar el resultado de la compra
data class CartUiState(
    val items: List<CartItemWithDetails> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val checkoutMessage: String? = null, // Mensaje de éxito o error
    val isCheckoutSuccess: Boolean = false
) {
    fun formattedTotal(): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
        format.maximumFractionDigits = 0
        return format.format(totalAmount)
    }
}

class CartViewModel(private val repository: AppRepository) : ViewModel() {

    // Combinamos el flujo del repo con un flujo local para mensajes
    private val _checkoutState = MutableStateFlow<Pair<String?, Boolean>>(null to false)

    val uiState: StateFlow<CartUiState> = combine(
        repository.cartItems,
        _checkoutState
    ) { items, (msg, success) ->
        val total = items.sumOf { it.product.price * it.cartItem.quantity }
        CartUiState(
            items = items,
            totalAmount = total,
            isLoading = false,
            checkoutMessage = msg,
            isCheckoutSuccess = success
        )
    }.stateIn(
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

    // NUEVA FUNCIÓN DE CHECKOUT REAL
    fun performCheckout() {
        viewModelScope.launch {
            // 1. Llamar al repositorio
            val result = repository.checkout()

            // 2. Actualizar estado según resultado
            result.fold(
                onSuccess = { message ->
                    _checkoutState.value = message to true
                },
                onFailure = { error ->
                    _checkoutState.value = (error.message ?: "Error al procesar pago") to false
                }
            )
        }
    }

    fun consumeCheckoutMessage() {
        _checkoutState.value = null to false
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