package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.dao.CartItemWithDetails
import com.example.levelup_gamerpractica.data.model.OrderResponse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Estado completo de la UI del Carrito.
 * Contiene la lista de productos, el total a pagar y el estado del proceso de compra.
 */
data class CartUiState(
    val items: List<CartItemWithDetails> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    // Estados específicos del proceso de compra (Checkout)
    val checkoutError: String? = null,
    val checkoutSuccess: OrderResponse? = null
) {
    fun formattedTotal(): String {
        return "$${totalAmount.toLong()}"
    }
}

class CartViewModel(private val repository: AppRepository) : ViewModel() {

    // Estado interno para controlar el flujo del checkout (Idle, Loading, Success, Error)
    private val _checkoutState = MutableStateFlow<CheckoutStatus>(CheckoutStatus.Idle)

    // Combinamos el flujo de items del repositorio (Room) con el estado del checkout
    // para generar un único estado de UI reactivo.
    val uiState: StateFlow<CartUiState> = combine(
        repository.cartItems,
        _checkoutState
    ) { items, checkoutStatus ->

        // Calcular el total sumando (precio * cantidad) de cada item
        val total = items.sumOf { item ->
            parsePrice(item.price) * item.quantity.toDouble()
        }

        // Determinar valores de estado basados en el checkoutStatus
        val error = if (checkoutStatus is CheckoutStatus.Error) checkoutStatus.message else null
        val success = if (checkoutStatus is CheckoutStatus.Success) checkoutStatus.order else null
        // Mostramos carga si se están cargando items O si se está procesando el checkout
        val loading = checkoutStatus is CheckoutStatus.Loading

        CartUiState(
            items = items,
            totalAmount = total,
            isLoading = loading,
            checkoutError = error,
            checkoutSuccess = success
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CartUiState(isLoading = true)
    )

    // --- Acciones del Carrito ---

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

    /**
     * Inicia el proceso de compra llamando al Backend a través del repositorio.
     * Actualiza _checkoutState para reflejar el progreso.
     */
    fun performCheckout() {
        viewModelScope.launch {
            _checkoutState.value = CheckoutStatus.Loading

            // Llamada al repositorio (que llama a la API /api/orders/checkout)
            val result = repository.checkout()

            result.onSuccess { order ->
                _checkoutState.value = CheckoutStatus.Success(order)
            }.onFailure { e ->
                _checkoutState.value = CheckoutStatus.Error(e.message ?: "Error desconocido en checkout")
            }
        }
    }

    /**
     * Limpia el estado de checkout.
     * Útil para llamar después de mostrar un Toast de éxito o navegar fuera de la pantalla,
     * para que el estado no se quede "pegado" en Success o Error.
     */
    fun resetCheckoutStatus() {
        _checkoutState.value = CheckoutStatus.Idle
    }

    /**
     * Helper para limpiar el string de precio (ej: "$ 50.000" -> 50000.0)
     */
    private fun parsePrice(value: String): Double {
        val cleaned = value.replace(Regex("[^0-9]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}

// Clase sellada auxiliar para gestionar internamente los estados del checkout
sealed class CheckoutStatus {
    object Idle : CheckoutStatus()
    object Loading : CheckoutStatus()
    data class Success(val order: OrderResponse) : CheckoutStatus()
    data class Error(val message: String) : CheckoutStatus()
}

/**
 * Factory para poder inyectar el AppRepository en el constructor del ViewModel.
 */
class CartViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}