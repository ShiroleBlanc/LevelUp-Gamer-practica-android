package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.entities.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la UI
data class ProfileUiState(
    val user: User? = null, // Aquí vive el usuario con sus puntos y rol
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class ProfileViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Campos para los formularios de edición (Dialogs)
    val username = MutableStateFlow("")
    val email = MutableStateFlow("")
    val oldPassword = MutableStateFlow("")
    val newPassword = MutableStateFlow("")
    val confirmNewPassword = MutableStateFlow("")

    init {
        // 1. Observar al usuario actual desde el Repositorio (Room)
        // Esto mantiene la UI sincronizada con la base de datos local
        viewModelScope.launch {
            repository.currentUser.collectLatest { user ->
                _uiState.update { it.copy(user = user) }

                // Si hay usuario, pre-llenamos los campos de edición
                if (user != null) {
                    username.value = user.username
                    email.value = user.email
                }
            }
        }

        // 2. Refrescar datos desde el Backend al iniciar
        // Esto asegura que veamos los puntos y nivel más recientes
        refreshProfile()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Llama a la función que creamos en AppRepository
            val success = repository.loadUserProfile()

            if (!success) {
                // No mostramos error crítico, solo dejamos los datos locales
                _uiState.update { it.copy(isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Actualizar Foto (Llamada al Repo)
    fun updateProfilePicture(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
            val result = repository.updateProfilePicture(uri)
            _uiState.update {
                if (result.isSuccess) it.copy(isLoading = false, isSuccess = true)
                else it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    // Actualizar Datos de Texto (Llamada al Repo)
    fun updateDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
            val result = repository.updateUserDetails(username.value, email.value)
            _uiState.update {
                if (result.isSuccess) it.copy(isLoading = false, isSuccess = true)
                else it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    // Actualizar Contraseña (Llamada al Repo)
    fun updatePassword() {
        if (newPassword.value != confirmNewPassword.value) {
            _uiState.update { it.copy(error = "Las contraseñas no coinciden") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
            // Nota: Para cambiar contraseña real en backend, necesitarías un endpoint en la API
            // y una función en el repositorio 'updatePasswordApi'.
            // Por ahora usamos la local.
            val result = repository.updateUserPassword(oldPassword.value, newPassword.value)
            _uiState.update {
                if (result.isSuccess) it.copy(isLoading = false, isSuccess = true)
                else it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logoutUser()
        }
    }

    fun consumeUiState() {
        _uiState.update { it.copy(error = null, isSuccess = false) }
    }
}

class ProfileViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}