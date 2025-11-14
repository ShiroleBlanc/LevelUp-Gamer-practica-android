package com.example.levelup_gamerpractica.data.remote

import retrofit2.http.GET

interface ApiService {

    @GET("api/products")
    suspend fun getAllProducts(): List<ProductNetworkDto>
}