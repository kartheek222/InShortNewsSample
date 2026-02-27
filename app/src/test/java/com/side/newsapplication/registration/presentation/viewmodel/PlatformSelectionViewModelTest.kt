package com.onexp.remag.registration.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.runtime.mutableStateListOf
import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.MasterPlatformResponseItem
import com.onexp.remag.registration.domain.usecase.PlatformSelectionUseCase
import com.onexp.remag.registration.domain.usecase.SetPlatformUseCase
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
 * Created by Sonu.Sinha  on 3/28/2024
 */
@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class PlatformSelectionViewModelTest {

    lateinit var apiServices: ApiServices
    lateinit var mockedContext: Context
    lateinit var systemUnderTest: PlatformSelectionViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        mockedContext = mockk<Context>(relaxed = true)
        systemUnderTest = PlatformSelectionViewModel(
            PlatformSelectionUseCase(apiServices),
            SetPlatformUseCase(apiServices)
        )
    }

    @Test
    fun `validate network error`() = runTest() {
        coEvery { apiServices.requestPlatforms() } throws NoNetworkException()
        // stateList is for collecting the Ui State of _gamerIdResponse in view-model.
        val stateList =
            mutableStateListOf<PlatformSelectionViewModel.PlatformResponseUiState>()
        systemUnderTest.platformResponse.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //api call
        systemUnderTest.requestPlatforms()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is PlatformSelectionViewModel.PlatformResponseUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is PlatformSelectionViewModel.PlatformResponseUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is PlatformSelectionViewModel.PlatformResponseUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.platformResponse.value is PlatformSelectionViewModel.PlatformResponseUiState.StateSError && (systemUnderTest.platformResponse.value as PlatformSelectionViewModel.PlatformResponseUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate requestPlatforms with error response`() = runTest {
        val errorResponseModel: BaseResponseModel<List<MasterPlatformResponseItem>?> =
            ErrorResponseModel(
                data = listOf(
                    MasterPlatformResponseItem(
                        "test",
                        "test", 101
                    )
                )
            )
        coEvery { apiServices.requestPlatforms() } returns errorResponseModel
        val stateList =
            mutableStateListOf<PlatformSelectionViewModel.PlatformResponseUiState>()
        systemUnderTest.platformResponse.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestPlatforms()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is PlatformSelectionViewModel.PlatformResponseUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is PlatformSelectionViewModel.PlatformResponseUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is PlatformSelectionViewModel.PlatformResponseUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.platformResponse.value is PlatformSelectionViewModel.PlatformResponseUiState.StateSError
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.platformResponse.value as PlatformSelectionViewModel.PlatformResponseUiState.StateSError).type as PlatformSelectionViewModel.PlatformSelectionUIStateType.PlateformResponse).response == errorResponseModel
        ).isTrue()

    }

    @Test
    fun `validate setPlatform with error response`() = runTest {
        val errorResponseModel = ErrorResponseModel<Unit>()
        coEvery { apiServices.setSelectedPlatform(any()) } returns errorResponseModel
        val stateList =
            mutableStateListOf<PlatformSelectionViewModel.PlatformResponseUiState>()
        systemUnderTest.platformResponse.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestSetPlatforms()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is PlatformSelectionViewModel.PlatformResponseUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is PlatformSelectionViewModel.PlatformResponseUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is PlatformSelectionViewModel.PlatformResponseUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.platformResponse.value is PlatformSelectionViewModel.PlatformResponseUiState.StateSError
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.platformResponse.value as PlatformSelectionViewModel.PlatformResponseUiState.StateSError).type as PlatformSelectionViewModel.PlatformSelectionUIStateType.SetSelectedPlatform).response == errorResponseModel
        ).isTrue()

    }

    @Test
    fun `validate requestPlatform with success response`() = runTest {
        val successResponseModel: BaseResponseModel<List<MasterPlatformResponseItem>?> = SuccessResponseModel(
            data = listOf(
                MasterPlatformResponseItem(
                    "test",
                    "test", 101
                )
            )
        )
        coEvery { apiServices.requestPlatforms() } returns successResponseModel
        val stateList =
            mutableStateListOf<PlatformSelectionViewModel.PlatformResponseUiState>()
        systemUnderTest.platformResponse.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestPlatforms()
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is PlatformSelectionViewModel.PlatformResponseUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is PlatformSelectionViewModel.PlatformResponseUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is PlatformSelectionViewModel.PlatformResponseUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(systemUnderTest.platformResponse.value is PlatformSelectionViewModel.PlatformResponseUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(
            ((systemUnderTest.platformResponse.value as PlatformSelectionViewModel.PlatformResponseUiState.StateSuccess).type as PlatformSelectionViewModel.PlatformSelectionUIStateType.PlateformResponse).response == successResponseModel
        ).isTrue()
    }

    @Test
    fun `validate setPlatform with success response`() = runTest {
        val successResponseModel = SuccessResponseModel(data = Unit)
        coEvery { apiServices.setSelectedPlatform(any()) } returns successResponseModel
        val stateList =
            mutableStateListOf<PlatformSelectionViewModel.PlatformResponseUiState>()
        systemUnderTest.platformResponse.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestSetPlatforms()
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is PlatformSelectionViewModel.PlatformResponseUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is PlatformSelectionViewModel.PlatformResponseUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is PlatformSelectionViewModel.PlatformResponseUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(systemUnderTest.platformResponse.value is PlatformSelectionViewModel.PlatformResponseUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(
            ((systemUnderTest.platformResponse.value as PlatformSelectionViewModel.PlatformResponseUiState.StateSuccess).type as PlatformSelectionViewModel.PlatformSelectionUIStateType.SetSelectedPlatform).response == successResponseModel
        ).isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}