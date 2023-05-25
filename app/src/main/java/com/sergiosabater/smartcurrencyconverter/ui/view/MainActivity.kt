package com.sergiosabater.smartcurrencyconverter.ui.view

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sergiosabater.smartcurrencyconverter.data.network.RetrofitClient
import com.sergiosabater.smartcurrencyconverter.database.AppDatabase
import com.sergiosabater.smartcurrencyconverter.domain.model.CurrencyResult
import com.sergiosabater.smartcurrencyconverter.domain.usecase.common.NavigateToSettingsUseCase
import com.sergiosabater.smartcurrencyconverter.repository.CurrencyRepository
import com.sergiosabater.smartcurrencyconverter.repository.CurrencyRepositoryImpl
import com.sergiosabater.smartcurrencyconverter.repository.datasource.LocalDataSource
import com.sergiosabater.smartcurrencyconverter.repository.datasource.RemoteDataSource
import com.sergiosabater.smartcurrencyconverter.ui.components.CurrencySelector
import com.sergiosabater.smartcurrencyconverter.ui.components.Display
import com.sergiosabater.smartcurrencyconverter.ui.components.Keyboard
import com.sergiosabater.smartcurrencyconverter.ui.components.SplashScreen
import com.sergiosabater.smartcurrencyconverter.ui.components.config.KeyboardConfig
import com.sergiosabater.smartcurrencyconverter.ui.theme.SmartCurrencyConverterTheme
import com.sergiosabater.smartcurrencyconverter.util.parser.CurrencyApiHelper
import com.sergiosabater.smartcurrencyconverter.util.parser.CurrencyApiHelperImpl
import com.sergiosabater.smartcurrencyconverter.viewmodel.MainViewModel
import com.sergiosabater.smartcurrencyconverter.viewmodel.MainViewModelFactory
import com.sergiosabater.smartcurrencyconverter.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiInterface = RetrofitClient.instance
        val remoteDataSource = RemoteDataSource(apiInterface)
        val database = AppDatabase.getDatabase(this)
        val currencyRateDao = database.currencyRateDao()
        val localDataSource = LocalDataSource(currencyRateDao)
        val currencyRepository = CurrencyRepositoryImpl(remoteDataSource, localDataSource)
        val currencyApiHelper = CurrencyApiHelperImpl()
        val settingsViewModel = SettingsViewModel(application)

        setContent {
            SmartCurrencyConverterTheme {
                val navController = rememberNavController()

                NavHost(navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            currencyRepository,
                            navController,
                            currencyApiHelper,
                            settingsViewModel
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    currencyRepository: CurrencyRepository,
    navController: NavController,
    currencyApiHelper: CurrencyApiHelper,
    settingsViewModel: SettingsViewModel
) {
    val navigateToSettingsUseCase = NavigateToSettingsUseCase(navController)
    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(
            LocalContext.current.applicationContext as Application,
            currencyRepository,
            navigateToSettingsUseCase,
            currencyApiHelper,
            settingsViewModel
        )
    )

    // Recolectamos los StateFlow del ViewModel como un State en Compose
    val uiState by mainViewModel.uiState.collectAsState()

    val mDisplay = Display()
    val mCurrencySelector = CurrencySelector()
    val mKeyboard = Keyboard()
    val keyboardConfig = KeyboardConfig()
    val splashScreen = SplashScreen()


    when (uiState.currencies) {

        is CurrencyResult.Loading -> {
            splashScreen.Splash()
        }

        is CurrencyResult.Success -> {
            val currenciesList = (uiState.currencies as CurrencyResult.Success).data
            Column {
                // Primer display
                mDisplay.CustomDisplay(
                    displayText = uiState.displayText,
                    symbol = uiState.displaySymbol
                )

                // Divider crea una línea horizontal entre los dos displays
                Divider(color = Color.Gray, thickness = 2.dp)

                // Segundo display
                mDisplay.CustomDisplay(
                    displayText = uiState.conversionResult,
                    symbol = uiState.conversionSymbol
                )

                // Selector de monedas
                mCurrencySelector.CustomCurrencySelector(
                    currenciesList,
                    mainViewModel::onCurrencySelected,
                    uiState.selectedCurrency1 ?: currenciesList[0],
                    uiState.selectedCurrency2 ?: currenciesList[0]
                )

                //Teclado
                mKeyboard.CustomKeyboard(
                    config = keyboardConfig,
                    onClearButtonClick = mainViewModel::onClearButtonClicked,
                    onNumericButtonClicked = mainViewModel::onNumericButtonClicked,
                    onBackspaceClicked = mainViewModel::onBackspaceClicked,
                    onSettingsButtonClicked = mainViewModel::onSettingsButtonClicked,
                    onKeyClicked = mainViewModel::onKeyClicked
                )
            }
        }

        is CurrencyResult.Failure -> {
            ErrorScreen()
        }
    }
}

