package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.entities.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(private val repository: AppRepository) : ViewModel() {

    val username = MutableStateFlow("")
    val email = MutableStateFlow("")
    val oldPassword = MutableStateFlow("")
    val newPassword = MutableStateFlow("")
    val confirmNewPassword = MutableStateFlow("")

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
                user?.let {
                    username.value = it.username
                    email.value = it.email
                }
            }
        }
    }

    private fun hashPassword(password: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Resetea el estado de error/éxito para ocultar mensajes
     */
    fun consumeUiState() {
        _uiState.value = _uiState.value.copy(isSuccess = false, error = null)
    }

    /**
     * Llama al repositorio para actualizar la foto de perfil
     */
    fun updateProfilePicture(uriString: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.updateProfilePicture(uriString)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    /**
     * Llama al repositorio para actualizar el nombre de usuario y email
     */
    fun updateDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.updateUserDetails(
                newUsername = username.value,
                newEmail = email.value
            )
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    /**
     * Llama al repositorio para actualizar la contraseña
     */
    fun updatePassword() {
        if (newPassword.value != confirmNewPassword.value) {
            _uiState.value = _uiState.value.copy(error = "Las nuevas contraseñas no coinciden.")
            return
        }
        if (newPassword.value.length < 6) {
            _uiState.value = _uiState.value.copy(error = "La nueva contraseña debe tener al menos 6 caracteres.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            // 2. Hashear contraseñas
            val oldHash = hashPassword(oldPassword.value)
            val newHash = hashPassword(newPassword.value)
            val result = repository.updateUserPassword(
                oldPasswordHash = oldHash,
                newPasswordHash = newHash
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                oldPassword.value = ""
                newPassword.value = ""
                confirmNewPassword.value = ""
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
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
