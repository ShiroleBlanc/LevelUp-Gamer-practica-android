package com.example.levelup_gamerpractica.data.model

import com.example.levelup_gamerpractica.data.remote.ProductNetworkDto

// --- CARRITO ---

data class CartResponse(
    val id: Long,
    val items: List<CartItemResponse>,
    val total: Double
)

data class CartItemResponse(
    val id: Long,
    val product: ProductNetworkDto, // Reutilizamos el DTO de producto existente
    val quantity: Int
)

data class CartItemRequest(
    val productId: Int,
    val quantity: Int
)

data class UpdateCartItemRequest(
    val quantity: Int
)

// --- ÓRDENES (PEDIDOS) ---

data class OrderResponse(
    val id: Long,
    val orderDate: String,
    val originalPrice: Double,
    val finalPrice: Double,
    val pointsEarned: Int,
    val orderItems: List<OrderItemResponse>
)

data class OrderItemResponse(
    val id: Long,
    val product: ProductNetworkDto,
    val quantity: Int,
    val priceAtPurchase: Double
)