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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AppRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setup() {
        repository = mockk()
        viewModel = RegisterViewModel(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `register success updates uiState to Success`() = runTest {
        val username = "GamerPro"
        val email = "test@duocuc.cl"
        val password = "password123"
        val birthDate = LocalDate.of(2000, 1, 1)

        coEvery { repository.registerUserApi(any()) } returns Result.success("Ok")

        viewModel.onUsernameChange(username)
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(password)
        viewModel.onBirthDateChange(birthDate)

        viewModel.register()

        assertTrue(viewModel.uiState.value is RegisterUiState.Success)

        coVerify(exactly = 1) {
            repository.registerUserApi(match {
                it.username == username && it.email == email && it.dateOfBirth == "2000-01-01"
            })
        }
    }

    @Test
    fun `register failure updates uiState to Error`() = runTest {
        // GIVEN
        viewModel.onUsernameChange("GamerPro")
        viewModel.onEmailChange("test@duocuc.cl")
        viewModel.onPasswordChange("123456")
        viewModel.onConfirmPasswordChange("123456")
        viewModel.onBirthDateChange(LocalDate.of(1990, 1, 1))

        val errorMsg = "El usuario ya existe"
        coEvery { repository.registerUserApi(any()) } returns Result.failure(Exception(errorMsg))

        viewModel.register()

        val state = viewModel.uiState.value
        assertTrue(state is RegisterUiState.Error)
        assertEquals(errorMsg, (state as RegisterUiState.Error).message)
    }

    @Test
    fun `validation fails if passwords do not match`() = runTest {
        viewModel.onPasswordChange("123456")
        viewModel.onConfirmPasswordChange("654321")

        viewModel.onUsernameChange("ValidUser")
        viewModel.onEmailChange("a@a.com")
        viewModel.onBirthDateChange(LocalDate.of(1990, 1, 1))

        viewModel.register()

        assertTrue(viewModel.uiState.value is RegisterUiState.Idle)
        assertNotNull(viewModel.confirmPasswordError.value)
        assertEquals("Las contraseñas no coinciden.", viewModel.confirmPasswordError.value)

        coVerify(exactly = 0) { repository.registerUserApi(any()) }
    }

    @Test
    fun `validation fails if user is underage`() = runTest {
        viewModel.onBirthDateChange(LocalDate.now())

        viewModel.onUsernameChange("ValidUser")
        viewModel.onEmailChange("a@a.com")
        viewModel.onPasswordChange("123456")
        viewModel.onConfirmPasswordChange("123456")

        viewModel.register()

        assertNotNull(viewModel.birthDateError.value)
        assertEquals("Debes ser mayor de 18 años.", viewModel.birthDateError.value)

        coVerify(exactly = 0) { repository.registerUserApi(any()) }
    }

    @Test
    fun `validation fails if username is too short`() = runTest {
        viewModel.onUsernameChange("abc")

        viewModel.onEmailChange("a@a.com")
        viewModel.onPasswordChange("123456")
        viewModel.onConfirmPasswordChange("123456")
        viewModel.onBirthDateChange(LocalDate.of(1990, 1, 1))

        viewModel.register()

        assertNotNull(viewModel.usernameError.value)
        coVerify(exactly = 0) { repository.registerUserApi(any()) }
    }
}