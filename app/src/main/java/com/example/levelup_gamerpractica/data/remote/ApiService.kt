package com.example.levelup_gamerpractica.data.remote

import com.example.levelup_gamerpractica.data.model.LoginRequest
import com.example.levelup_gamerpractica.data.model.LoginResponse
import com.example.levelup_gamerpractica.data.model.ProfileResponse
import com.example.levelup_gamerpractica.data.model.RegisterRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @GET("api/products")
    suspend fun getAllProducts(): List<ProductNetworkDto>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>

    @GET("api/profile/me")
    suspend fun getUserProfile(): Response<ProfileResponse>

    // --- ¡NUEVO MÉTODO PARA SUBIR FOTO! ---
    @Multipart
    @POST("api/profile/picture")
    suspend fun uploadProfilePicture(
        @Part file: MultipartBody.Part
    ): Response<Map<String, String>> // El backend devuelve {"profilePictureUrl": "..."}
}