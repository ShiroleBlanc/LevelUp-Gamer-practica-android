package com.example.levelup_gamerpractica.data.remote

import retrofit2.http.GET

interface ApiService {

    // Llama al endpoint GET /api/products
    @GET("api/products")
    suspend fun getAllProducts(): List<ProductNetworkDto>

    // Aquí añadirías luego:
    // @GET("api/products/{id}")
    // suspend fun getProductById(@Path("id") id: Long): ProductNetworkDto
}