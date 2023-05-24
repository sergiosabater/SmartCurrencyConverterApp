package com.sergiosabater.smartcurrencyconverter.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.sergiosabater.smartcurrencyconverter.data.network.ApiResult
import com.sergiosabater.smartcurrencyconverter.domain.model.CurrencyResult
import com.sergiosabater.smartcurrencyconverter.domain.usecase.common.NavigateToSettingsUseCase
import com.sergiosabater.smartcurrencyconverter.repository.CurrencyRepository
import com.sergiosabater.smartcurrencyconverter.util.parser.CurrencyApiHelperImpl
import com.sergiosabater.smartcurrencyconverter.viewmodel.TestHelpers.generateCurrencyList
import com.sergiosabater.smartcurrencyconverter.viewmodel.TestHelpers.response
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    // Dependencias mockeadas
    private lateinit var application: Application
    private lateinit var mainViewModel: MainViewModel

    private val mockCurrencyRepository = mockk<CurrencyRepository>()
    private val mockNavigateToSettingsUseCase = mockk<NavigateToSettingsUseCase>()
    private val mockCurrencyApiHelper = mockk<CurrencyApiHelperImpl>()


    @Before
    fun setUp() {

        // Mockea el contexto de la aplicación
        application = mockk(relaxed = true) {
            every { applicationContext } returns this
        }

        // Cada vez que se llame a loadCurrenciesFromApi, devuelve la lista de Currency predeterminada
        every {
            mockCurrencyApiHelper.loadCurrenciesFromApi(
                any(),
                any()
            )
        } returns generateCurrencyList()

        coEvery { mockCurrencyRepository.getCurrencyRates() } returns ApiResult.Success(response)

        // Crea la instancia del ViewModel con las dependencias mockeadas
        mainViewModel =
            MainViewModel(
                application,
                mockCurrencyRepository,
                mockNavigateToSettingsUseCase,
                mockCurrencyApiHelper
            )
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `test loadCurrencies`() = coroutineTestRule.runTest {

        // Creamos el resultado esperado
        val expectedResult = CurrencyResult.Success(generateCurrencyList())

        // Given:
        // Configurar las respuestas de las dependencias mockeadas
        coEvery { mockCurrencyRepository.getCurrencyRates() } returns ApiResult.Success(response)

        // When:
        // Llamar al método a testear
        mainViewModel.loadCurrencies()

        // Then:
        // Los resultados obtenidos
        mainViewModel.currencies.test {
            assertTrue(awaitItem() == expectedResult)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `test onClearButtonClicked`() = coroutineTestRule.runTest {

        // Given
        mainViewModel.onNumericButtonClicked("8")

        // When
        mainViewModel.onClearButtonClicked()

        // Then
        mainViewModel.displayText.test {
            assertTrue(awaitItem() == "0")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `test onNumericButtonClicked with existing display value`() = coroutineTestRule.runTest {
        // Given a display text with value "1.000"
        mainViewModel.onNumericButtonClicked("1")
        mainViewModel.onNumericButtonClicked("0")
        mainViewModel.onNumericButtonClicked("0")
        mainViewModel.onNumericButtonClicked("0")

        // When
        mainViewModel.onNumericButtonClicked("3")

        // Then
        mainViewModel.displayText.test {
            assertTrue(awaitItem() == "10.003")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `test onBackspaceClicked with existing display value`() = coroutineTestRule.runTest {
        // Given a display text with value "123"
        mainViewModel.onNumericButtonClicked("1")
        mainViewModel.onNumericButtonClicked("2")
        mainViewModel.onNumericButtonClicked("3")

        // When
        mainViewModel.onBackspaceClicked()

        // Then
        mainViewModel.displayText.test {
            assertTrue(awaitItem() == "12")
            cancelAndIgnoreRemainingEvents()
        }
    }

}