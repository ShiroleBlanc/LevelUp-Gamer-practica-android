package com.example.levelup_gamerpractica.data.remote

import com.example.levelup_gamerpractica.data.model.LoginRequest
import com.example.levelup_gamerpractica.data.model.LoginResponse
import com.example.levelup_gamerpractica.data.model.RegisterRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // Obtener todos los productos (Ya debías tener algo similar)
    @GET("api/products")
    suspend fun getAllProducts(): List<ProductNetworkDto>

    // Endpoint para LOGIN
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Endpoint para REGISTRO
    // Usamos ResponseBody porque el backend devuelve un texto simple, no un JSON complejo
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>
}