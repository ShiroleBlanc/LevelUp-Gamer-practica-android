package com.example.levelup_gamerpractica.viewmodel

import com.example.levelup_gamerpractica.MainDispatcherRule
import com.example.levelup_gamerpractica.data.local.AppRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.example.levelup_gamerpractica.data.local.entities.User

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    // Regla para manejar hilos de corrutinas en los tests
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AppRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        // 1. Creamos el Mock del repositorio
        repository = mockk()
        // 2. Inicializamos el ViewModel con el mock
        viewModel = LoginViewModel(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `login success updates uiState to Success`() = runTest {
        // GIVEN - Datos válidos
        val username = "AdminUser"
        val password = "securePassword"

        // SOLUCIÓN: Creamos un usuario falso (mockeado) en lugar de un String
        // 'relaxed = true' permite que el usuario tenga valores por defecto si se le piden propiedades
        val fakeUser = mockk<User>(relaxed = true)

        // Ahora devolvemos Result.success(fakeUser) en vez de un String
        coEvery { repository.loginUserApi(username, password) } returns Result.success(fakeUser)

        viewModel.onUsernameChange(username)
        viewModel.onPasswordChange(password)

        // WHEN - Ejecutamos login
        viewModel.login()

        // THEN - Verificamos estado Success
        assertTrue(viewModel.uiState.value is LoginUiState.Success)

        // Verificamos que se llamó al repositorio
        coVerify(exactly = 1) { repository.loginUserApi(username, password) }
    }

    @Test
    fun `login failure updates uiState to Error`() = runTest {
        // GIVEN - Datos válidos pero credenciales incorrectas
        val username = "AdminUser"
        val password = "wrongPassword"
        val errorMsg = "Credenciales inválidas"

        // Simulamos que el repositorio responde con Fallo
        coEvery { repository.loginUserApi(any(), any()) } returns Result.failure(Exception(errorMsg))

        viewModel.onUsernameChange(username)
        viewModel.onPasswordChange(password)

        // WHEN
        viewModel.login()

        // THEN
        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals(errorMsg, (state as LoginUiState.Error).message)
    }

    @Test
    fun `validation fails if fields are empty`() = runTest {
        // GIVEN - Campos vacíos
        viewModel.onUsernameChange("") // Username vacío
        viewModel.onPasswordChange("123456")

        // WHEN
        viewModel.login()

        // THEN
        val state = viewModel.uiState.value

        // 1. Debe ser estado de Error
        assertTrue(state is LoginUiState.Error)

        // 2. El mensaje debe ser el de validación local
        assertEquals("Completa todos los campos.", (state as LoginUiState.Error).message)

        // 3. CRUCIAL: El repositorio NUNCA debe ser llamado si la validación falla
        coVerify(exactly = 0) { repository.loginUserApi(any(), any()) }
    }

    @Test
    fun `validation fails if password is empty`() = runTest {
        // GIVEN - Password vacío
        viewModel.onUsernameChange("UserTest")
        viewModel.onPasswordChange("   ") // Espacios en blanco cuentan como vacío por el isBlank()

        // WHEN
        viewModel.login()

        // THEN
        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals("Completa todos los campos.", (state as LoginUiState.Error).message)

        coVerify(exactly = 0) { repository.loginUserApi(any(), any()) }
    }
}