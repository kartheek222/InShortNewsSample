package com.side.newsapplication.registration.presentation.viewmodel

import android.content.Context
import com.google.common.truth.Truth
import com.google.gson.JsonElement
import com.onexp.remag.MainDispatcherExtension
import com.side.newsapplication.data.ErrorResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.domain.utils.PKCEUtil
import com.onexp.remag.registration.domain.usecase.BioSetupUseCase
import com.onexp.remag.registration.domain.usecase.SetProfileCompleteUseCase
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.preferences.BasePreferencesManager
import com.onexp.remag.repository.preferences.PreferencesManager
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class FtueBioSetupViewModelTest {
    private lateinit var mockedContext: Context
    private lateinit var ftueBioSetupViewModel: FtueBioSetupViewModel
    private lateinit var apiServices: ApiServices
    private lateinit var generalPreferences: BasePreferencesManager
    private lateinit var setProfileCompleteUseCase: SetProfileCompleteUseCase

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        apiServices = mockk<ApiServices>()
        generalPreferences = mockk<PreferencesManager>(relaxed = true)
        setProfileCompleteUseCase = SetProfileCompleteUseCase(apiServices)
        ftueBioSetupViewModel = FtueBioSetupViewModel(
            BioSetupUseCase(mockedContext, apiServices),
            setProfileCompleteUseCase,
            generalPreferences
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request bio api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.bioSetup(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<FtueBioSetupViewModel.BioSetupUiState>()
        ftueBioSetupViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        ftueBioSetupViewModel.submitBioSetup("fakeBio")
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateError::class.java)
        //Checking the exception
        Truth.assertThat(
            (ftueBioSetupViewModel.uiState.value is FtueBioSetupViewModel.BioSetupUiState.StateError)
                    && (ftueBioSetupViewModel.uiState.value as FtueBioSetupViewModel.BioSetupUiState.StateError).exception is NoNetworkException
        ).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request bio api with error response case`() = runTest {
        val errorResponseModel = ErrorResponseModel<JsonElement?>()

        //Mocking the response
        coEvery { apiServices.bioSetup(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<FtueBioSetupViewModel.BioSetupUiState>()
        ftueBioSetupViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        ftueBioSetupViewModel.submitBioSetup("fakeBio")
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateError::class.java)
        //Checking the exception
        if ((ftueBioSetupViewModel.uiState.value as FtueBioSetupViewModel.BioSetupUiState.StateError).type is FtueBioSetupViewModel.BioSetupUiStateType.StateRequestBioSetup) {
            Truth.assertThat(
                ((ftueBioSetupViewModel.uiState.value as FtueBioSetupViewModel.BioSetupUiState.StateError).type as FtueBioSetupViewModel.BioSetupUiStateType.StateRequestBioSetup).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request bio api error case with empty data`() = runTest {
        val errorResponseModel = ErrorResponseModel<JsonElement?>()
        coEvery { apiServices.bioSetup(any()) } returns errorResponseModel

        val sateList = mutableListOf<FtueBioSetupViewModel.BioSetupUiState>()
        ftueBioSetupViewModel.uiState.onEach(sateList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        ftueBioSetupViewModel.submitBioSetup("fake bio")
        advanceUntilIdle()
        Truth.assertThat(sateList).hasSize(3)
        Truth.assertThat(sateList[0])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateNone::class.java)
        Truth.assertThat(sateList[1])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateLoading::class.java)
        Truth.assertThat(sateList[2])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateError::class.java)
        Truth.assertThat((ftueBioSetupViewModel.uiState.value as FtueBioSetupViewModel.BioSetupUiState.StateError).type is FtueBioSetupViewModel.BioSetupUiStateType.StateRequestBioSetup)
            .isTrue()
        //Checking the error type in the uiState
        if ((ftueBioSetupViewModel.uiState.value as FtueBioSetupViewModel.BioSetupUiState.StateError).type is FtueBioSetupViewModel.BioSetupUiStateType.StateRequestBioSetup) {
            Truth.assertThat(
                ((ftueBioSetupViewModel.uiState.value as FtueBioSetupViewModel.BioSetupUiState.StateError).type as FtueBioSetupViewModel.BioSetupUiStateType.StateRequestBioSetup).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request bio api success data`() = runTest {
        val successResponseModel = SuccessResponseModel<JsonElement?>()
        coEvery { apiServices.bioSetup(any()) } returns successResponseModel
        coEvery { apiServices.setAvatar() } returns successResponseModel

        val sateList = mutableListOf<FtueBioSetupViewModel.BioSetupUiState>()
        ftueBioSetupViewModel.uiState.onEach(sateList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        ftueBioSetupViewModel.submitBioSetup("fake bio")
        advanceUntilIdle()
        Truth.assertThat(sateList).hasSize(5)
        Truth.assertThat(sateList[0])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateNone::class.java)
        Truth.assertThat(sateList[1])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateLoading::class.java)
        Truth.assertThat(sateList[2])
            .isInstanceOf(FtueBioSetupViewModel.BioSetupUiState.StateSuccess::class.java)
        Truth.assertThat((ftueBioSetupViewModel.uiState.value as FtueBioSetupViewModel.BioSetupUiState.StateSuccess).type is FtueBioSetupViewModel.BioSetupUiStateType.SetOboardingComplete)
            .isTrue()
        //Checking the error type in the uiState
        if ((ftueBioSetupViewModel.uiState.value as FtueBioSetupViewModel.BioSetupUiState.StateSuccess).type is FtueBioSetupViewModel.BioSetupUiStateType.SetOboardingComplete) {
            Truth.assertThat(
                ((ftueBioSetupViewModel.uiState.value as FtueBioSetupViewModel.BioSetupUiState.StateSuccess).type as FtueBioSetupViewModel.BioSetupUiStateType.SetOboardingComplete).response?.equals(
                    successResponseModel
                )
            ).isTrue()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(PKCEUtil)
    }

}