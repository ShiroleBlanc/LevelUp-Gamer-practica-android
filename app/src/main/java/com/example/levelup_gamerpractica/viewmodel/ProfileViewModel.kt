package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.entities.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest

// --- ESTADO DE LA UI ---
// Define todos los campos que la ProfileScreen necesita
data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

// --- VIEWMODEL ---
class ProfileViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Observa al usuario actual desde el repositorio
        viewModelScope.launch {
            repository.currentUser.collectLatest { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    // Función para cambiar la foto de perfil
    fun onProfilePictureChanged(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.updateProfilePicture(uri)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    // Función para actualizar nombre de usuario o email
    fun updateDetails(username: String? = null, email: String? = null) {
        viewModelScope.launch {
            if (username != null && username.isBlank()) {
                _uiState.update { it.copy(error = "El nombre de usuario no puede estar vacío") }
                return@launch
            }
            if (email != null && email.isBlank()) {
                _uiState.update { it.copy(error = "El email no puede estar vacío") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val result = repository.updateUserDetails(
                newUsername = username?.trim(),
                newEmail = email?.trim()
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    // Función para actualizar la contraseña
    fun updatePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            if (oldPassword.isBlank() || newPassword.isBlank()) {
                _uiState.update { it.copy(error = "Las contraseñas no pueden estar vacías") }
                return@launch
            }
            if (newPassword.length < 6) {
                _uiState.update { it.copy(error = "La nueva contraseña debe tener al menos 6 caracteres") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            // --- CORRECCIÓN DE NOMBRES DE PARÁMETROS ---
            val result = repository.updateUserPassword(
                oldPassword = hashPassword(oldPassword), // No 'oldPasswordHash'
                newPassword = hashPassword(newPassword)  // No 'newPasswordHash'
            )
            // --- FIN DE LA CORRECCIÓN ---

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    // Función para "consumir" (limpiar) el estado de error o éxito
    fun consumeUiState() {
        _uiState.update { it.copy(error = null, isSuccess = false) }
    }

    // Helper para hashear contraseñas (igual que en RegisterViewModel)
    private fun hashPassword(password: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}

// --- FACTORY ---
class ProfileViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}