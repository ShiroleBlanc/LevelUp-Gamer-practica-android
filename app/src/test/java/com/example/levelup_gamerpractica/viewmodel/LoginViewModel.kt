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
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AppRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        repository = mockk()
        viewModel = LoginViewModel(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `login success updates uiState to Success`() = runTest {
        val username = "AdminUser"
        val password = "securePassword"

        val fakeUser = mockk<User>(relaxed = true)

        coEvery { repository.loginUserApi(username, password) } returns Result.success(fakeUser)

        viewModel.onUsernameChange(username)
        viewModel.onPasswordChange(password)

        viewModel.login()

        assertTrue(viewModel.uiState.value is LoginUiState.Success)

        coVerify(exactly = 1) { repository.loginUserApi(username, password) }
    }

    @Test
    fun `login failure updates uiState to Error`() = runTest {
        val username = "AdminUser"
        val password = "wrongPassword"
        val errorMsg = "Credenciales inválidas"

        coEvery { repository.loginUserApi(any(), any()) } returns Result.failure(Exception(errorMsg))

        viewModel.onUsernameChange(username)
        viewModel.onPasswordChange(password)

        viewModel.login()

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals(errorMsg, (state as LoginUiState.Error).message)
    }

    @Test
    fun `validation fails if fields are empty`() = runTest {
        viewModel.onUsernameChange("")
        viewModel.onPasswordChange("123456")

        viewModel.login()

        val state = viewModel.uiState.value

        assertTrue(state is LoginUiState.Error)

        assertEquals("Completa todos los campos.", (state as LoginUiState.Error).message)

        coVerify(exactly = 0) { repository.loginUserApi(any(), any()) }
    }

    @Test
    fun `validation fails if password is empty`() = runTest {
        viewModel.onUsernameChange("UserTest")
        viewModel.onPasswordChange("   ")

        viewModel.login()

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals("Completa todos los campos.", (state as LoginUiState.Error).message)

        coVerify(exactly = 0) { repository.loginUserApi(any(), any()) }
    }
}