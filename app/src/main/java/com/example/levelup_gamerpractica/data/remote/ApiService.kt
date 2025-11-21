package com.example.levelup_gamerpractica.data.remote

import com.example.levelup_gamerpractica.data.model.LoginRequest
import com.example.levelup_gamerpractica.data.model.LoginResponse
import com.example.levelup_gamerpractica.data.model.ProfileResponse // <-- Importar
import com.example.levelup_gamerpractica.data.model.RegisterRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("api/products")
    suspend fun getAllProducts(): List<ProductNetworkDto>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>

    // --- ¡NUEVO ENDPOINT! ---
    // Obtiene los datos reales del usuario (puntos, nivel, foto, etc.)
    // El token se envía automáticamente gracias al AuthInterceptor
    @GET("api/profile/me")
    suspend fun getUserProfile(): Response<ProfileResponse>
}