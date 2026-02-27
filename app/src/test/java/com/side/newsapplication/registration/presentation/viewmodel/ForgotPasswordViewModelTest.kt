package com.side.newsapplication.registration.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.onexp.remag.MainDispatcherExtension
import com.side.newsapplication.data.ERROR_MESSAGE_SESSION_EXPIRED
import com.side.newsapplication.data.ErrorResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.domain.utils.PKCEUtil
import com.onexp.remag.registration.data.GenerateForgotPasswordResponseModel
import com.onexp.remag.registration.domain.usecase.GenerateForgotPasswordOtpUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import com.onexp.remag.repository.preferences.BasePreferencesManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class ForgotPasswordViewModelTest{
    private lateinit var mockedContext: Context
    private lateinit var forgotPasswordViewModel: ForgotPasswordViewModel
    private lateinit var apiServices: ApiServices
    private lateinit var preferencesManager: BasePreferencesManager

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        apiServices = mockk<ApiServices>()
        mockkObject(PKCEUtil)
        every { PKCEUtil.generateCodeVerifier() } returns "fakeCodeVerifier"
        every { PKCEUtil.generateCodeChallenge(any()) } returns "fakeCodeVerifier"

        preferencesManager = mockk(relaxed = true)
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceId"

        mockkStatic(FirebaseMessaging::class)
        every { FirebaseMessaging.getInstance() } returns mockk(relaxed = true)

        forgotPasswordViewModel = ForgotPasswordViewModel(
            GenerateForgotPasswordOtpUseCase(mockedContext, apiServices)
        )
    }
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Settings.Secure::class)
        unmockkStatic(FirebaseMessaging::class)
        unmockkObject(PKCEUtil)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request otp api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.generateForgotPasswordOtp(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<ForgotPasswordViewModel.ForgotPasswordUiState>()
        forgotPasswordViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        forgotPasswordViewModel.requestOtp(
                "fakeEmailOrGamerId"
            )
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(
            (forgotPasswordViewModel.uiState.value is ForgotPasswordViewModel.ForgotPasswordUiState.StateSError)
                    && (forgotPasswordViewModel.uiState.value as ForgotPasswordViewModel.ForgotPasswordUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request otp api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<GenerateForgotPasswordResponseModel?>()
        coEvery { apiServices.generateForgotPasswordOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<ForgotPasswordViewModel.ForgotPasswordUiState>()
        forgotPasswordViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        forgotPasswordViewModel.requestOtp(
            "fakeEmailOrGamerId"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(forgotPasswordViewModel.uiState.value is ForgotPasswordViewModel.ForgotPasswordUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (forgotPasswordViewModel.uiState.value as ForgotPasswordViewModel.ForgotPasswordUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request otp api with session time out error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<GenerateForgotPasswordResponseModel?>(
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            status = ApiConstants.STATUS.IAT,
            message = ERROR_MESSAGE_SESSION_EXPIRED
        )
        coEvery { apiServices.generateForgotPasswordOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<ForgotPasswordViewModel.ForgotPasswordUiState>()
        forgotPasswordViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        forgotPasswordViewModel.requestOtp(
            "fakeEmailOrGamerId"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateSessionTimeOut::class.java)
        //Checking the exception
        Truth.assertThat(forgotPasswordViewModel.uiState.value is ForgotPasswordViewModel.ForgotPasswordUiState.StateSessionTimeOut)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            ((forgotPasswordViewModel.uiState.value as ForgotPasswordViewModel.ForgotPasswordUiState.StateSessionTimeOut).type as ForgotPasswordViewModel.ForgotPasswordUiStateType.StateOtpRequested).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }
    @Test
    fun `verify request otp api error case with empty data`() = runTest {
        val errorResponseModel = ErrorResponseModel<GenerateForgotPasswordResponseModel?>()
        coEvery { apiServices.generateForgotPasswordOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<ForgotPasswordViewModel.ForgotPasswordUiState>()
        forgotPasswordViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        forgotPasswordViewModel.requestOtp(
            "fakeEmailOrGamerId"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateSError::class.java)
        //Checking the state in uiState
        Truth.assertThat(forgotPasswordViewModel.uiState.value is ForgotPasswordViewModel.ForgotPasswordUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (forgotPasswordViewModel.uiState.value as ForgotPasswordViewModel.ForgotPasswordUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }
    @Test
    fun `verify request otp api with success case`() = runTest {
        mockkStatic(FirebaseCrashlytics::class)
        //Mocking the fake response
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        coEvery { apiServices.generateForgotPasswordOtp(any()) } returns SuccessResponseModel(
            GenerateForgotPasswordResponseModel(
                authCode = "test",
                email = "test",
            )
        )
        //it will contain states list
        val uiStatesList = mutableListOf<ForgotPasswordViewModel.ForgotPasswordUiState>()

        //Collecting the ui state
        forgotPasswordViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api
        forgotPasswordViewModel.requestOtp(
            "fakeEmailOrGamerId"
        )

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(uiStatesList).hasSize(3)
        Truth.assertThat(uiStatesList[0]).isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateNone::class.java)
        Truth.assertThat(uiStatesList[1]).isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateLoading::class.java)
        Truth.assertThat(uiStatesList[2]).isInstanceOf(ForgotPasswordViewModel.ForgotPasswordUiState.StateSuccess::class.java)

        Truth.assertThat((forgotPasswordViewModel.uiState.value as ForgotPasswordViewModel.ForgotPasswordUiState.StateSuccess).type is ForgotPasswordViewModel.ForgotPasswordUiStateType.StateOtpRequested)
            .isTrue()
        unmockkStatic(FirebaseCrashlytics::class)
    }
}