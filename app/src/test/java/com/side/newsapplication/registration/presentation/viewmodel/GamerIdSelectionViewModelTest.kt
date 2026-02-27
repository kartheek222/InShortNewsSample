package com.side.newsapplication.registration.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.runtime.mutableStateListOf
import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.side.newsapplication.data.ErrorResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.GamerIdSelectionResponse
import com.onexp.remag.registration.domain.usecase.ReserveGamerIdUseCase
import com.onexp.remag.registration.domain.usecase.ValidateGamerIdUseCase
import com.onexp.remag.repository.network.ApiServices
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.manipulation.Ordering.Context

/**
 * Created by Sonu.Sinha  on 2/5/2024
 */
@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class GamerIdSelectionViewModelTest {

    lateinit var apiServices: ApiServices
    lateinit var mockedContext: Context
    lateinit var systemUnderTest: GamerIdSelectionViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        mockedContext = mockk<Context>(relaxed = true)
        systemUnderTest = GamerIdSelectionViewModel(
            ValidateGamerIdUseCase(apiServices),
            ReserveGamerIdUseCase(apiServices)
        )
    }

    @Test
    fun `validateFields with empty values`() = runTest {
        val testResult = systemUnderTest.validateGamerIdField("")
        Truth.assertThat(testResult is GamerIdSelectionViewModel.ValidationErrorType.Username)
            .isTrue()

    }

    @Test
    fun `validateFields with correct value`() {
        val testResult = systemUnderTest.validateGamerIdField("test")
        Truth.assertThat(testResult is GamerIdSelectionViewModel.ValidationErrorType.Success)
            .isTrue()
    }

    @Test
    fun `validate network error`() = runTest() {
        coEvery { apiServices.validateGamerId(any()) } throws NoNetworkException()
        // stateList is for collecting the Ui State of _gamerIdResponse in view-model.
        val stateList =
            mutableStateListOf<GamerIdSelectionViewModel.GamerIdRegistrationUiState>()
        systemUnderTest.gamerIdResponse.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //api call
        systemUnderTest.requestSearchGamerId("test")
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.gamerIdResponse.value is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSError && (systemUnderTest.gamerIdResponse.value as GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate gamerIdSearch with error response`() = runTest {
        val errorResponseModel = ErrorResponseModel<GamerIdSelectionResponse>()
        coEvery { apiServices.validateGamerId(any()) } returns errorResponseModel
        val stateList =
            mutableStateListOf<GamerIdSelectionViewModel.GamerIdRegistrationUiState>()
        systemUnderTest.gamerIdResponse.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestSearchGamerId("test")
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.gamerIdResponse.value is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSError
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.gamerIdResponse.value as GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSError).type as GamerIdSelectionViewModel.GamerIdRegistrationUIStateType.GamerIdSearch).response == errorResponseModel
        ).isTrue()

    }

    @Test
    fun `validate gamerIdReserve with error response`() = runTest {
        val errorResponseModel = ErrorResponseModel<Unit>()
        coEvery { apiServices.reserveGamerId(any()) } returns errorResponseModel
        val stateList =
            mutableStateListOf<GamerIdSelectionViewModel.GamerIdRegistrationUiState>()
        systemUnderTest.gamerIdResponse.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.reserveGamerId("test")
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.gamerIdResponse.value is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSError
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.gamerIdResponse.value as GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSError).type as GamerIdSelectionViewModel.GamerIdRegistrationUIStateType.GamerIdReserve).response == errorResponseModel
        ).isTrue()

    }

    @Test
    fun `validate gamerIdSearch with success response`() = runTest {
        val successResponseModel = SuccessResponseModel(
            data = GamerIdSelectionResponse(
                "test",
                true
            )
        )
        coEvery { apiServices.validateGamerId(any()) } returns successResponseModel
        val stateList =
            mutableStateListOf<GamerIdSelectionViewModel.GamerIdRegistrationUiState>()
        systemUnderTest.gamerIdResponse.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestSearchGamerId("test")
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(systemUnderTest.gamerIdResponse.value is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(
            ((systemUnderTest.gamerIdResponse.value as GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSuccess).type as GamerIdSelectionViewModel.GamerIdRegistrationUIStateType.GamerIdSearch).response == successResponseModel
        ).isTrue()
    }

    @Test
    fun `validate gamerIdReserve with success response`() = runTest {
        val successResponseModel = SuccessResponseModel(data = Unit)
        coEvery { apiServices.reserveGamerId(any()) } returns successResponseModel
        val stateList =
            mutableStateListOf<GamerIdSelectionViewModel.GamerIdRegistrationUiState>()
        systemUnderTest.gamerIdResponse.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.reserveGamerId("test")
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(systemUnderTest.gamerIdResponse.value is GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(
            ((systemUnderTest.gamerIdResponse.value as GamerIdSelectionViewModel.GamerIdRegistrationUiState.StateSuccess).type as GamerIdSelectionViewModel.GamerIdRegistrationUIStateType.GamerIdReserve).response == successResponseModel
        ).isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}