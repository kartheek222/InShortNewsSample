package com.side.newsapplication.registration.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import com.onexp.remag.registration.data.GenerateSignUpOtpResponseModel
import com.onexp.remag.registration.data.ValidateSignUpOtpResponseModel
import com.onexp.remag.registration.domain.usecase.GenerateForgotPasswordOtpUseCase
import com.onexp.remag.registration.domain.usecase.GenerateSignUpOtpUseCase
import com.onexp.remag.registration.domain.usecase.ValidateForgotPasswordOtpUseCase
import com.onexp.remag.registration.domain.usecase.ValidateSignupOtpUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServiceBuilder
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import com.onexp.remag.repository.preferences.BasePreferencesManager
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
class OneTimeVerificationViewmodelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var mockedContext: Context
    private lateinit var oneTimeVerificationViewModel: OneTimeVerificationViewmodel
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
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceid"

        mockkStatic(FirebaseMessaging::class)
        every { FirebaseMessaging.getInstance() } returns mockk(relaxed = true)
        mockkObject(ApiServiceBuilder)
        every {
            ApiServiceBuilder.getApiServicesWithUrl(any(), any())
        } returns apiServices

        oneTimeVerificationViewModel = OneTimeVerificationViewmodel(
            GenerateSignUpOtpUseCase(mockedContext, apiServices),
            GenerateForgotPasswordOtpUseCase(mockedContext, apiServices),
            ValidateSignupOtpUseCase(mockedContext, apiServices),
            ValidateForgotPasswordOtpUseCase(mockedContext, apiServices)
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
    fun `verify request signupOtp api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.generateSignUpOtp() } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Making the API request
        oneTimeVerificationViewModel.requestSignUpOtp()

        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
                    && (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `verify request signupOtp api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<GenerateSignUpOtpResponseModel?>()
        coEvery { apiServices.generateSignUpOtp() } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.requestSignUpOtp()

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the state in uiState
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify request signupOtp api error case with empty data`() = runTest {
        val errorResponseModel = ErrorResponseModel<GenerateSignUpOtpResponseModel?>()
        coEvery { apiServices.generateSignUpOtp() } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.requestSignUpOtp()

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the state in uiState
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify request signupOtp api with session time out error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<GenerateSignUpOtpResponseModel?>(
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            status = ApiConstants.STATUS.IAT,
            message = ERROR_MESSAGE_SESSION_EXPIRED
        )
        coEvery { apiServices.generateSignUpOtp() } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.requestSignUpOtp()

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut::class.java)
        //Checking the state in uiState
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            ((oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut).type as OneTimeVerificationViewmodel.OneTimeVerificationUiStateType.StateSignUpOtpRequested).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

//    @Test
//    fun `verify request signupOtp api with success case`() = runTest {
//        mockkStatic(FirebaseCrashlytics::class)
//        //Mocking the fake response
//        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)
//
//        coEvery { apiServices.generateSignUpOtp() } returns SuccessResponseModel()
//        //it will contain states list
//        val uiStatesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
//
//        //Collecting the ui state
//        oneTimeVerificationViewModel.uiState
//            .onEach { uiStatesList.add(it) }
//            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
//
//        //Mocking the api
//        oneTimeVerificationViewModel.requestSignUpOtp()
//
//        //Wait until the response is executed.
//        advanceUntilIdle()
//
//        //Check the list size and values inside it.
//        Truth.assertThat(uiStatesList).hasSize(3)
//        Truth.assertThat(uiStatesList[0])
//            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
//        Truth.assertThat(uiStatesList[1])
//            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
//        Truth.assertThat(uiStatesList[2])
//            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSuccess::class.java)
//
//        Truth.assertThat((oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSuccess).type is OneTimeVerificationViewmodel.OneTimeVerificationUiStateType.StateSignUpOtpRequested)
//            .isTrue()
//        unmockkStatic(FirebaseCrashlytics::class)
//    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request forgotPasswordOtp api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.generateForgotPasswordOtp(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.requestForgotPasswordOtp(
            "fakeEmailOrGamerId"
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
                    && (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request forgotPasswordOtp api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<GenerateForgotPasswordResponseModel?>()
        coEvery { apiServices.generateForgotPasswordOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.requestForgotPasswordOtp(
            "fakeEmailOrGamerId"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify request forgotPasswordOtp api error case with empty data`() = runTest {
        val errorResponseModel = ErrorResponseModel<GenerateForgotPasswordResponseModel?>()
        coEvery { apiServices.generateForgotPasswordOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.requestForgotPasswordOtp(
            "fakeEmailOrGamerId"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the state in uiState
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify request forgotPasswordOtp api with session time out error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<GenerateForgotPasswordResponseModel?>(
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            status = ApiConstants.STATUS.IAT,
            message = ERROR_MESSAGE_SESSION_EXPIRED
        )
        coEvery { apiServices.generateForgotPasswordOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.requestForgotPasswordOtp(
            "fakeEmailOrGamerId"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut::class.java)
        //Checking the state in uiState
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            ((oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut).type as OneTimeVerificationViewmodel.OneTimeVerificationUiStateType.StateForgotPasswordOtpRequested).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

//    @Test
//    fun `verify request forgotPasswordOtp api with success case`() = runTest {
//        mockkStatic(FirebaseCrashlytics::class)
//        //Mocking the fake response
//        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)
//
//        coEvery { apiServices.generateForgotPasswordOtp(any()) } returns SuccessResponseModel(
//            GenerateForgotPasswordResponseModel(
//                authCode = "test",
//                email = "test",
//            )
//        )
//        //it will contain states list
//        val uiStatesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
//
//        //Collecting the ui state
//        oneTimeVerificationViewModel.uiState
//            .onEach { uiStatesList.add(it) }
//            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
//
//        //Mocking the api
//        oneTimeVerificationViewModel.requestForgotPasswordOtp(
//            "fakeEmailOrGamerId"
//        )
//
//        //Wait until the response is executed.
//        advanceUntilIdle()
//
//        //Check the list size and values inside it.
//        Truth.assertThat(uiStatesList).hasSize(3)
//        Truth.assertThat(uiStatesList[0])
//            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
//        Truth.assertThat(uiStatesList[1])
//            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
//        Truth.assertThat(uiStatesList[2])
//            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSuccess::class.java)
//
//        Truth.assertThat((oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSuccess).type is OneTimeVerificationViewmodel.OneTimeVerificationUiStateType.StateForgotPasswordOtpRequested)
//            .isTrue()
//        unmockkStatic(FirebaseCrashlytics::class)
//    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify validate forgotPasswordOtp api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.validateForgotPasswordOtp(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.validateForgotPasswordOtp(
            "fakeOtp"
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
                    && (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify validate forgotPasswordOtp api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>()
        coEvery { apiServices.validateForgotPasswordOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.validateForgotPasswordOtp(
            "fakeOtp"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify validate forgotPasswordOtp api error case with empty data`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>()
        coEvery { apiServices.validateForgotPasswordOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.validateForgotPasswordOtp(
            "fakeOtp"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the state in uiState
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify validate forgotPasswordOtp api with session time out error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>(
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            status = ApiConstants.STATUS.IAT,
            message = ERROR_MESSAGE_SESSION_EXPIRED
        )
        coEvery { apiServices.validateForgotPasswordOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.validateForgotPasswordOtp(
            "fakeOtp"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut::class.java)
        //Checking the state in uiState
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            ((oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut).type as OneTimeVerificationViewmodel.OneTimeVerificationUiStateType.StateForgotPasswordOtpValidate).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify validate forgotPasswordOtp api with success case`() = runTest {
        mockkStatic(FirebaseCrashlytics::class)
        //Mocking the fake response
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        coEvery { apiServices.validateForgotPasswordOtp(any()) } returns SuccessResponseModel()
        //it will contain states list
        val uiStatesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()

        //Collecting the ui state
        oneTimeVerificationViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api
        oneTimeVerificationViewModel.validateForgotPasswordOtp(
            "fakeOtp"
        )

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(uiStatesList).hasSize(3)
        Truth.assertThat(uiStatesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(uiStatesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(uiStatesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSuccess::class.java)

        Truth.assertThat((oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSuccess).type is OneTimeVerificationViewmodel.OneTimeVerificationUiStateType.StateForgotPasswordOtpValidate)
            .isTrue()
        unmockkStatic(FirebaseCrashlytics::class)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify validate signUpOtp api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.validateSignUpOtp(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.validateSignUpOtp(
            "fakeOtp"
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
                    && (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify validate signUpOtp api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<ValidateSignUpOtpResponseModel?>()
        coEvery { apiServices.validateSignUpOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.validateSignUpOtp(
            "fakeOtp"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify validate signUpOtp api error case with empty data`() = runTest {
        val errorResponseModel = ErrorResponseModel<ValidateSignUpOtpResponseModel?>()
        coEvery { apiServices.validateSignUpOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.validateSignUpOtp(
            "fakeOtp"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError::class.java)
        //Checking the state in uiState
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify validate signUpOtp api with session time out error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<ValidateSignUpOtpResponseModel?>(
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            status = ApiConstants.STATUS.IAT,
            message = ERROR_MESSAGE_SESSION_EXPIRED
        )
        coEvery { apiServices.validateSignUpOtp(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()
        oneTimeVerificationViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        oneTimeVerificationViewModel.validateSignUpOtp(
            "fakeOtp"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut::class.java)
        //Checking the state in uiState
        Truth.assertThat(oneTimeVerificationViewModel.uiState.value is OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            ((oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSessionTimeOut).type as OneTimeVerificationViewmodel.OneTimeVerificationUiStateType.StateSignUpOtpValidate).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify validate signUpOtp api with success case`() = runTest {
        mockkStatic(FirebaseCrashlytics::class)
        //Mocking the fake response
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        coEvery { apiServices.validateSignUpOtp(any()) } returns SuccessResponseModel(
            ValidateSignUpOtpResponseModel(
                status = "test"
            )
        )
        //it will contain states list
        val uiStatesList = mutableListOf<OneTimeVerificationViewmodel.OneTimeVerificationUiState>()

        //Collecting the ui state
        oneTimeVerificationViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api
        oneTimeVerificationViewModel.validateSignUpOtp(
            "fakeOtp"
        )

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(uiStatesList).hasSize(3)
        Truth.assertThat(uiStatesList[0])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateNone::class.java)
        Truth.assertThat(uiStatesList[1])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateLoading::class.java)
        Truth.assertThat(uiStatesList[2])
            .isInstanceOf(OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSuccess::class.java)

        Truth.assertThat((oneTimeVerificationViewModel.uiState.value as OneTimeVerificationViewmodel.OneTimeVerificationUiState.StateSuccess).type is OneTimeVerificationViewmodel.OneTimeVerificationUiStateType.StateSignUpOtpValidate)
            .isTrue()
        unmockkStatic(FirebaseCrashlytics::class)
    }

}