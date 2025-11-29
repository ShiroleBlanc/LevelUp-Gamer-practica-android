package com.example.levelup_gamerpractica.data.remote

import com.example.levelup_gamerpractica.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("api/products")
    suspend fun getAllProducts(): List<ProductNetworkDto>

    // --- AUTH ---
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>

    // --- PROFILE ---
    @GET("api/profile/me")
    suspend fun getUserProfile(): Response<ProfileResponse>

    @Multipart
    @POST("api/profile/picture")
    suspend fun uploadProfilePicture(@Part file: MultipartBody.Part): Response<Map<String, String>>

    @PUT("api/profile/me")
    suspend fun updateProfile(@Body updates: Map<String, String>): Response<ProfileResponse>

    // --- CARRITO (NUEVO) ---
    @GET("api/cart")
    suspend fun getMyCart(): Response<CartResponse>

    @POST("api/cart/items")
    suspend fun addItemToCart(@Body request: CartItemRequest): Response<CartResponse>

    // Usamos un Map para el body porque el backend espera un objeto JSON {"quantity": X}
    @PUT("api/cart/items/{productId}")
    suspend fun updateCartItem(@Path("productId") productId: Int, @Body body: Map<String, Int>): Response<CartResponse>

    @DELETE("api/cart/items/{productId}")
    suspend fun removeCartItem(@Path("productId") productId: Int): Response<CartResponse>

    @DELETE("api/cart")
    suspend fun clearCartApi(): Response<CartResponse>

    // --- ÓRDENES / CHECKOUT (NUEVO) ---
    @POST("api/orders/checkout")
    suspend fun checkout(): Response<OrderResponse>

    @GET("api/orders/my-orders")
    suspend fun getMyOrders(): Response<List<OrderResponse>>
}