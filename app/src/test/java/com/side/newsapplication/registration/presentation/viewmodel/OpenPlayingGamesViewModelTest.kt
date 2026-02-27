package com.onexp.remag.registration.presentation.viewmodel

import android.provider.Settings
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ERROR_MESSAGE_SESSION_EXPIRED
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.domain.usecase.OpenToPlayGamesUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServiceBuilder
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class OpenPlayingGamesViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var openPlayingGamesViewModel: OpenPlayingGamesViewModel
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceid"

        mockkStatic(FirebaseMessaging::class)
        every { FirebaseMessaging.getInstance() } returns mockk(relaxed = true)
        mockkObject(ApiServiceBuilder)
        every {
            ApiServiceBuilder.getApiServicesWithUrl(any(), any())
        } returns apiServices

        openPlayingGamesViewModel = OpenPlayingGamesViewModel(
            OpenToPlayGamesUseCase(apiServices)
        )
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify submitOpenToPlayGames api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.submitOpenToPlayGames(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<OpenPlayingGamesViewModel.OpenToPlayGameUiState>()
        openPlayingGamesViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Making the API request
        openPlayingGamesViewModel.submitOpenToPlayGames()

        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(
            (openPlayingGamesViewModel.uiState.value is OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSError)
                    && (openPlayingGamesViewModel.uiState.value as OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `verify submitOpenToPlayGames api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>()
        coEvery { apiServices.submitOpenToPlayGames(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OpenPlayingGamesViewModel.OpenToPlayGameUiState>()
        openPlayingGamesViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        openPlayingGamesViewModel.submitOpenToPlayGames()

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSError::class.java)
        //Checking the state in uiState
        Truth.assertThat(openPlayingGamesViewModel.uiState.value is OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (openPlayingGamesViewModel.uiState.value as OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify submitOpenToPlayGames api error case with empty data`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>()
        coEvery { apiServices.submitOpenToPlayGames(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OpenPlayingGamesViewModel.OpenToPlayGameUiState>()
        openPlayingGamesViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        openPlayingGamesViewModel.submitOpenToPlayGames()

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSError::class.java)
        //Checking the state in uiState
        Truth.assertThat(openPlayingGamesViewModel.uiState.value is OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (openPlayingGamesViewModel.uiState.value as OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }
    @Test
    fun `verify submitOpenToPlayGames api with session time out error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>(
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            status = ApiConstants.STATUS.IAT,
            message = ERROR_MESSAGE_SESSION_EXPIRED
        )
        coEvery { apiServices.submitOpenToPlayGames(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OpenPlayingGamesViewModel.OpenToPlayGameUiState>()
        openPlayingGamesViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        openPlayingGamesViewModel.submitOpenToPlayGames()

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSessionTimeOut::class.java)
        //Checking the state in uiState
        Truth.assertThat(openPlayingGamesViewModel.uiState.value is OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSessionTimeOut)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            ((openPlayingGamesViewModel.uiState.value as OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSessionTimeOut).type as OpenPlayingGamesViewModel.OpenToPlayGameUiStateType.OpenToPlayResponseState).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify submitOpenToPlayGames api with success case`() = runTest {
        mockkStatic(FirebaseCrashlytics::class)
        //Mocking the fake response
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        coEvery { apiServices.submitOpenToPlayGames(any()) } returns SuccessResponseModel()
        //it will contain states list
        val uiStatesList = mutableListOf<OpenPlayingGamesViewModel.OpenToPlayGameUiState>()

        //Collecting the ui state
        openPlayingGamesViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api
        openPlayingGamesViewModel.submitOpenToPlayGames()

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(uiStatesList).hasSize(3)
        Truth.assertThat(uiStatesList[0])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateNone::class.java)
        Truth.assertThat(uiStatesList[1])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateLoading::class.java)
        Truth.assertThat(uiStatesList[2])
            .isInstanceOf(OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSuccess::class.java)

        Truth.assertThat((openPlayingGamesViewModel.uiState.value as OpenPlayingGamesViewModel.OpenToPlayGameUiState.StateSuccess).type is OpenPlayingGamesViewModel.OpenToPlayGameUiStateType.OpenToPlayResponseState)
            .isTrue()
        unmockkStatic(FirebaseCrashlytics::class)
    }

}