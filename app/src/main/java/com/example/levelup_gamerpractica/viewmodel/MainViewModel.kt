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

/**
 * ViewModel para el Scaffold principal.
 * Se encarga de conocer el estado de autenticación (quién está logueado)
 * y de manejar el cierre de sesión.
 */
class MainViewModel(private val repository: AppRepository) : ViewModel() {

    // Expone el Flow del usuario actual, convirtiéndolo a StateFlow
    // (Arreglo para el error de mismatch Flow/StateFlow)
    val currentUser: StateFlow<User?> = repository.currentUser.stateIn(
        scope = viewModelScope, // El scope del ViewModel
        started = SharingStarted.WhileSubscribed(5000), // Inicia cuando la UI observa
        initialValue = null // Asume null al inicio (no logueado)
    )

    // Función para cerrar la sesión
    fun logout() {
        viewModelScope.launch {
            repository.logoutUser()
            // La navegación se manejará en el Composable observando `currentUser`
        }
    }
}

// Factory para el MainViewModel
class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T // (Corregido un typo aquí también)
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
