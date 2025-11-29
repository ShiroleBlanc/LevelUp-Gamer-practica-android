package com.example.levelup_gamerpractica.viewmodel

import com.example.levelup_gamerpractica.MainDispatcherRule
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.entities.Product
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AppRepository
    private lateinit var viewModel: CatalogViewModel

    private val allProductsFlow = MutableStateFlow<List<Product>>(emptyList())
    private val allCategoriesFlow = MutableStateFlow<List<String>>(emptyList())
    private val userFlow = MutableStateFlow<String?>(null)

    // --- DATOS DE PRUEBA CORREGIDOS ---
    private val product1 = Product(
        id = 1L, // Long
        name = "Mouse Gamer",
        price = 20.0, // Double
        category = "Accesorios",
        description = "Mouse RGB",
        imageUrl = "http://fake.url/img1.png", // <--- CAMBIO: imageUrl (antes era image)
        manufacturer = "Logitech",
        distributor = "LevelUp Dist"
    )

    private val product2 = Product(
        id = 2L, // Long
        name = "Teclado Mecánico",
        price = 50.0, // Double
        category = "Accesorios",
        description = "Teclado Blue Switch",
        imageUrl = "http://fake.url/img2.png", // <--- CAMBIO: imageUrl
        manufacturer = "Razer",
        distributor = "LevelUp Dist"
    )

    private val product3 = Product(
        id = 3L, // Long
        name = "Elden Ring",
        price = 60.0, // Double
        category = "Juegos",
        description = "GOTY",
        imageUrl = "http://fake.url/img3.png", // <--- CAMBIO: imageUrl
        manufacturer = "FromSoftware",
        distributor = "Bandai"
    )

    @Before
    fun setup() {
        repository = mockk()

        // Configuramos mocks
        coEvery { repository.allProducts } returns allProductsFlow
        coEvery { repository.allCategories } returns allCategoriesFlow
        coEvery { repository.currentUserNameFlow } returns userFlow
        coJustRun { repository.refreshProducts() }
        coEvery { repository.getProductsByCategory(any()) } returns flowOf(emptyList())

        // Asegúrate de que tu Repositorio acepte Long en addToCart
        coJustRun { repository.addToCart(any()) }

        viewModel = CatalogViewModel(repository)
    }

    @Test
    fun `init block calls refreshProducts`() = runTest {
        coVerify(exactly = 1) { repository.refreshProducts() }
    }

    @Test
    fun `uiState emits correct initial data`() = runTest {
        // GIVEN
        val productsList = listOf(product1, product2, product3)
        val categoriesList = listOf("Accesorios", "Juegos")
        val userName = "Gamer123"

        allProductsFlow.value = productsList
        allCategoriesFlow.value = categoriesList
        userFlow.value = userName

        // Usamos collect {} vacío para mantener vivo el flujo
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        // THEN
        val state = viewModel.uiState.value

        assertEquals(3, state.products.size)
        assertEquals(productsList, state.products)
        assertEquals(listOf("Todos", "Accesorios", "Juegos"), state.categories)
        assertEquals("Todos", state.selectedCategory)
        assertEquals(userName, state.userName)
        assertFalse(state.isLoading)

        collectJob.cancel()
    }

    @Test
    fun `selectCategory filters products correctly`() = runTest {
        // GIVEN
        val accesoriosList = listOf(product1, product2)
        coEvery { repository.getProductsByCategory("Accesorios") } returns flowOf(accesoriosList)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        // WHEN
        viewModel.selectCategory("Accesorios")

        // THEN
        val state = viewModel.uiState.value
        assertEquals("Accesorios", state.selectedCategory)
        assertEquals(2, state.products.size)

        coVerify { repository.getProductsByCategory("Accesorios") }

        collectJob.cancel()
    }

    @Test
    fun `selectCategory 'Todos' returns all products`() = runTest {
        // GIVEN
        allProductsFlow.value = listOf(product1, product2, product3)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        viewModel.selectCategory("Juegos")
        viewModel.selectCategory("Todos")

        // THEN
        val state = viewModel.uiState.value
        assertEquals("Todos", state.selectedCategory)
        assertEquals(3, state.products.size)

        coVerify(atLeast = 1) { repository.allProducts }

        collectJob.cancel()
    }

    @Test
    fun `addToCart calls repository`() = runTest {
        // WHEN
        viewModel.addToCart(product1)

        // THEN
        // Verificamos que se llame con el ID Long (1L)
        coVerify(exactly = 1) { repository.addToCart(1L) }
    }
}