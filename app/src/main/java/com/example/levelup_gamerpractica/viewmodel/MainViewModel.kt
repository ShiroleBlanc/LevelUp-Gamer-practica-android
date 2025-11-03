package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.entities.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class MainViewModel(private val repository: AppRepository) : ViewModel() {

    val currentUser: StateFlow<User?> = repository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Función para cerrar la sesión
    fun logout() {
        viewModelScope.launch {
            repository.logoutUser()
        }
    }
}

// Factory para el MainViewModel
class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
