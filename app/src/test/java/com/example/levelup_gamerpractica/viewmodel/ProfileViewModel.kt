package com.example.levelup_gamerpractica.viewmodel

import com.example.levelup_gamerpractica.MainDispatcherRule
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.entities.User
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AppRepository
    private lateinit var viewModel: ProfileViewModel

    // Mock del flujo del usuario actual
    private val currentUserFlow = MutableStateFlow<User?>(null)

    // --- CORRECCIÓN AQUÍ: USUARIO COINCIDENTE CON TU MODELO ANDROID ---
    private val fakeUser = User(
        id = 1L,
        username = "GamerTest",
        email = "test@gmail.com",
        profilePictureUrl = null, // Puede ser null o un string URL
        userRole = "ROLE_USER",
        pointsBalance = 100,
        userLevel = 1
        // NOTA: Eliminamos password, dateOfBirth, etc. porque no existen en tu clase User de Android
    )
    // ------------------------------------------------------------------

    @Before
    fun setup() {
        repository = mockk()

        // Configurar mocks antes de instanciar el ViewModel
        coEvery { repository.currentUser } returns currentUserFlow
        coEvery { repository.loadUserProfile() } returns true

        viewModel = ProfileViewModel(repository)
    }

    @Test
    fun `init loads user profile from backend`() = runTest {
        coVerify(exactly = 1) { repository.loadUserProfile() }
    }

    @Test
    fun `when repository emits user, uiState and input fields are updated`() = runTest {
        // GIVEN
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        // WHEN
        currentUserFlow.value = fakeUser

        // THEN
        val state = viewModel.uiState.value
        assertNotNull(state.user)
        assertEquals("GamerTest", state.user?.username)

        // Verificamos pre-llenado de campos
        assertEquals("GamerTest", viewModel.username.value)
        assertEquals("test@gmail.com", viewModel.email.value)

        collectJob.cancel()
    }

    @Test
    fun `updateProfilePicture success`() = runTest {
        // GIVEN
        val imageUri = "content://images/123"
        coEvery { repository.updateProfilePicture(imageUri) } returns Result.success(Unit)

        // WHEN
        viewModel.updateProfilePicture(imageUri)

        // THEN
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isSuccess)
        assertNull(viewModel.uiState.value.error)

        coVerify { repository.updateProfilePicture(imageUri) }
    }

    @Test
    fun `updateDetails calls repository with correct values`() = runTest {
        // GIVEN
        viewModel.username.value = "NewName"
        viewModel.email.value = "new@mail.com"

        coEvery { repository.updateUserDetails("NewName", "new@mail.com") } returns Result.success(Unit)

        // WHEN
        viewModel.updateDetails()

        // THEN
        assertTrue(viewModel.uiState.value.isSuccess)
        coVerify { repository.updateUserDetails("NewName", "new@mail.com") }
    }

    @Test
    fun `updateDetails failure shows error`() = runTest {
        // GIVEN
        val errorMsg = "Email ya existe"
        coEvery { repository.updateUserDetails(any(), any()) } returns Result.failure(Exception(errorMsg))

        // WHEN
        viewModel.updateDetails()

        // THEN
        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertEquals(errorMsg, state.error)
    }

    @Test
    fun `updatePassword fails if passwords do not match`() = runTest {
        // GIVEN
        viewModel.newPassword.value = "123456"
        viewModel.confirmNewPassword.value = "654321"

        // WHEN
        viewModel.updatePassword()

        // THEN
        val state = viewModel.uiState.value
        assertEquals("Las contraseñas no coinciden", state.error)
        coVerify(exactly = 0) { repository.updateUserPassword(any(), any()) }
    }

    @Test
    fun `updatePassword success calls repository`() = runTest {
        // GIVEN
        viewModel.oldPassword.value = "oldPass"
        viewModel.newPassword.value = "newPass"
        viewModel.confirmNewPassword.value = "newPass"

        coEvery { repository.updateUserPassword("oldPass", "newPass") } returns Result.success(Unit)

        // WHEN
        viewModel.updatePassword()

        // THEN
        assertTrue(viewModel.uiState.value.isSuccess)
        coVerify(exactly = 1) { repository.updateUserPassword("oldPass", "newPass") }
    }

    @Test
    fun `logout calls repository`() = runTest {
        // GIVEN
        coJustRun { repository.logoutUser() }

        // WHEN
        viewModel.logout()

        // THEN
        coVerify(exactly = 1) { repository.logoutUser() }
    }
}