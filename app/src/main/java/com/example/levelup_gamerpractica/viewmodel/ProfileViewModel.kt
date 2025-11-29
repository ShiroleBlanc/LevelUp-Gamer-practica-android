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

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class ProfileViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val username = MutableStateFlow("")
    val email = MutableStateFlow("")
    val oldPassword = MutableStateFlow("")
    val newPassword = MutableStateFlow("")
    val confirmNewPassword = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.currentUser.collectLatest { user ->
                _uiState.update { it.copy(user = user) }

                if (user != null) {
                    username.value = user.username
                    email.value = user.email
                }
            }
        }

        refreshProfile()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = repository.loadUserProfile()

            if (!success) {
                _uiState.update { it.copy(isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

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

    fun updatePassword() {
        if (newPassword.value != confirmNewPassword.value) {
            _uiState.update { it.copy(error = "Las contraseñas no coinciden") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
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