@file:OptIn(ExperimentalCoroutinesApi::class)

package com.onexp.remag.registration.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SessionTimeoutResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.domain.ClearLoginPreferencesUseCase
import com.onexp.remag.registration.data.RegisterAuthTokenResponseModel
import com.onexp.remag.registration.domain.usecase.ParentalConsentPendingUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import com.onexp.remag.repository.preferences.BasePreferencesManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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

/**
 * Created by ismail.akhtar on 10-05-2024.
 */
@ExtendWith(MainDispatcherExtension::class)
class ParentalConsentPendingStateViewModelTest {

    private lateinit var parentalConsentPendingUseCase: ParentalConsentPendingUseCase
    private lateinit var apiServices: ApiServices
    private lateinit var sut : ParentalConsentPendingStateViewModel

    @BeforeEach
    fun setUp(){
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceId"
        apiServices = mockk<ApiServices>()
        val context = mockk<Context>()
        val preferencesManager = mockk<BasePreferencesManager>()
        parentalConsentPendingUseCase = ParentalConsentPendingUseCase(apiServices)
        sut = ParentalConsentPendingStateViewModel(parentalConsentPendingUseCase, ClearLoginPreferencesUseCase(
            context = context,
            generalPreferences = preferencesManager,
            encryptedPreferences = preferencesManager
        ))
    }

    @AfterEach
    fun tearDown(){
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `validate onboarding status api with network error case`() = runTest {
        //mocking response
        coEvery { apiServices.requestOnBoardingStatus() } throws NoNetworkException()

        //collecting uistates
        val uiStates = mutableListOf<ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState>()
        sut.uiState
            .onEach {
                uiStates.add(it)
            }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.requestOnBoardingStatus()

        //wait until all tasks are completed
        advanceUntilIdle()

        //checking the status of uistates
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.ErrorState::class.java)
        //checking network exception
        assertThat(
            (sut.uiState.value as ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.ErrorState).exception is NoNetworkException
        )
    }

    @Test
    fun `validate onboarding status api with error case`() = runTest {
        //mocking response
        coEvery { apiServices.requestOnBoardingStatus() } returns ErrorResponseModel()

        //collecting uiStates
        val uiStates = mutableListOf<ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.requestOnBoardingStatus()

        //wait until all tasks are completed
        advanceUntilIdle()

        //checking the status of uistates
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.ErrorState::class.java)
        //checking error
        val errorResponseModel = ErrorResponseModel<Any>()
        assertThat(
            (sut.uiState.value as ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.ErrorState).response?.equals(errorResponseModel)
        ).isTrue()
    }

    @Test
    fun `validate onboarding status api with session time out error case`() = runTest {
        //mocking response
        coEvery { apiServices.requestOnBoardingStatus() } returns SessionTimeoutResponseModel()

        //collecting uiStates
        val uiStates = mutableListOf<ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.requestOnBoardingStatus()

        //wait until all tasks are completed
        advanceUntilIdle()

        //checking the status of uistates
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.SessionTimeOutErrorState::class.java)
        //checking error
        val sessionTimeOutResponseModel = SessionTimeoutResponseModel<Any>()
        assertThat(
            (sut.uiState.value as ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.SessionTimeOutErrorState).response?.equals(sessionTimeOutResponseModel)
        ).isTrue()
        //checking the status code and status
        val response = (sut.uiState.value as ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.SessionTimeOutErrorState).response
        assertThat(
            response?.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE &&
            response.status == ApiConstants.STATUS.IAT
        ).isTrue()
    }

    @Test
    fun `validate onboarding status api with success case`() = runTest {
        val successResponseModel = SuccessResponseModel<RegisterAuthTokenResponseModel?>(
            data = RegisterAuthTokenResponseModel()
        )
        //mocking response
        coEvery { apiServices.requestOnBoardingStatus() } returns successResponseModel

        //collecting uiStates
        val uiStates = mutableListOf<ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.requestOnBoardingStatus()

        //wait until all tasks are completed
        advanceUntilIdle()

        //checking the status of uistates
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.SuccessState::class.java)
        //checking error
        assertThat(
            (sut.uiState.value as ParentalConsentPendingStateViewModel.ParentalConsentPendingUIState.SuccessState).response?.equals(successResponseModel)
        ).isTrue()
    }
}