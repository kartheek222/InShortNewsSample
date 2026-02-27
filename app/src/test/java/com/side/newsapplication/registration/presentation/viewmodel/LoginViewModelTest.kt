package com.side.newsapplication.registration.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.onexp.remag.MainDispatcherExtension
import com.side.newsapplication.data.ErrorResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.domain.utils.PKCEUtil
import com.onexp.remag.registration.data.LoginResponseModel
import com.onexp.remag.registration.data.LoginType
import com.onexp.remag.registration.data.RegisterAuthTokenResponseModel
import com.onexp.remag.registration.domain.usecase.RequestLoginUseCase
import com.onexp.remag.registration.domain.usecase.RequestOAuthTokenUseCase
import com.onexp.remag.registration.domain.usecase.SaveLoginPreferencesUseCase
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

/**
 * Created by kartheek.sabbisetty on 31-01-2024
 */
@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class LoginViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var mockedContext: Context
    private lateinit var loginViewModel: LoginViewModel
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

        loginViewModel = LoginViewModel(
            RequestLoginUseCase(context = mockedContext, apiServices = apiServices),
            RequestOAuthTokenUseCase(
                context = mockedContext
            ),
            saveLoginPreferencesUseCase = SaveLoginPreferencesUseCase(
                generalPreferences = preferencesManager,
                encryptedPreferences = preferencesManager
            ),
            generalPreferences = preferencesManager,
        )

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `'validateLoginFields with null and empty values'`() {
        var result = loginViewModel.validateLoginFields(null, null)
        assertThat(result is LoginViewModel.ValidationErrorType.Username).isTrue()

        result = loginViewModel.validateLoginFields(null, "test")
        assertThat(result is LoginViewModel.ValidationErrorType.Username).isTrue()

        result = loginViewModel.validateLoginFields("", "test")
        assertThat(result is LoginViewModel.ValidationErrorType.Username).isTrue()

        result = loginViewModel.validateLoginFields("   ", "test")
        assertThat(result is LoginViewModel.ValidationErrorType.Username).isTrue()


        result = loginViewModel.validateLoginFields("test", null)
        assertThat(result is LoginViewModel.ValidationErrorType.Password).isTrue()

        result = loginViewModel.validateLoginFields("test", "")
        assertThat(result is LoginViewModel.ValidationErrorType.Password).isTrue()

        result = loginViewModel.validateLoginFields("test", "   ")
        assertThat(result is LoginViewModel.ValidationErrorType.Password).isTrue()
    }

    @Test
    fun `'validateLoginFields with correct values return success'`() {
        val result = loginViewModel.validateLoginFields("test", "test")
        assertThat(result is LoginViewModel.ValidationErrorType.None).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request login api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.validateLogin(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Making the API request
        loginViewModel.requestLogin(
            userName = "fakeUserId",
            password = "fakePassword",
            loginType = LoginType.Email
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        assertThat(statesList).hasSize(3)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSError::class.java)
        //Checking the exception
        assertThat(
            (loginViewModel.uiState.value is LoginViewModel.LoginUiState.StateSError)
                    && (loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `verify request login api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<RegisterAuthTokenResponseModel?>()
        coEvery { apiServices.validateLogin(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        loginViewModel.requestLogin(
            userName = "fakeUserId",
            password = "fakePassword",
            loginType = LoginType.Email
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(statesList).hasSize(3)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSError::class.java)
        //Checking the state in uiState
        assertThat((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestLogin).isTrue()
        //Checking the error type in the uiState
        if ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestLogin) {
            assertThat(
                ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type as LoginViewModel.LoginUiStateType.StateRequestLogin).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }


    @Test
    fun `verify request login api error case with empty data`() = runTest {
        val errorResponseModel = SuccessResponseModel<RegisterAuthTokenResponseModel?>()
        coEvery { apiServices.validateLogin(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        loginViewModel.requestLogin(
            userName = "fakeUserId",
            password = "fakePassword",
            loginType = LoginType.Email
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(statesList).hasSize(3)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSError::class.java)
        //Checking the state in uiState
        assertThat((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestLogin).isTrue()
        //Checking the error type in the uiState
        if ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestLogin) {
            assertThat(
                ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type as LoginViewModel.LoginUiStateType.StateRequestLogin).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request oAuth token api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.requestOAuthToken(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        loginViewModel.requestOAuthTokens(
            RegisterAuthTokenResponseModel(
                userId = "",
                authCode = "",
                url = "", onboardingStatus = "", guardianEmailId = ""
            )
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        assertThat(statesList).hasSize(3)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSError::class.java)
        //Checking the exception
        assertThat(
            (loginViewModel.uiState.value is LoginViewModel.LoginUiState.StateSError)
                    && (loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `verify request  oAuthToken api failure case`() = runTest {
        val errorResponse = ErrorResponseModel<LoginResponseModel?>()
        coEvery { apiServices.requestOAuthToken(any()) } returns errorResponse

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        loginViewModel.requestOAuthTokens(
            RegisterAuthTokenResponseModel(
                userId = "",
                authCode = "",
                url = "", onboardingStatus = "", guardianEmailId = ""
            )
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        println("ui states list = $statesList")
        //Check the list size and values inside it.
        assertThat(statesList).hasSize(3)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSError::class.java)
        //Checking the state in uiState
        assertThat((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestAuthToken).isTrue()
        //Checking the error type in the uiState
        if ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestAuthToken) {
            assertThat(
                ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type as LoginViewModel.LoginUiStateType.StateRequestAuthToken).response?.equals(
                    errorResponse
                )
            ).isTrue()
        }
    }

    @Test
    fun `verify request login api success case`() = runTest {
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)
        val successResponse = SuccessResponseModel<LoginResponseModel?>(
            LoginResponseModel(
                accessToken = "test",
                refreshToken = "test", guardianEmail = ""
            ),
        )
        coEvery { apiServices.validateLogin(any()) } returns SuccessResponseModel(
            RegisterAuthTokenResponseModel(
                authCode = "test",
                url = "test",
                userId = "test", onboardingStatus = "", guardianEmailId = ""
            )
        )

        coEvery { apiServices.requestOAuthToken(any()) } returns successResponse

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        loginViewModel.requestLogin(
            userName = "fakeUserId",
            password = "fakePassword",
            loginType = LoginType.Email
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        println("login states : $statesList")
        //Check the list size and values inside it.
        assertThat(statesList).hasSize(5)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSuccess::class.java)
        assertThat(statesList[3]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[4]).isInstanceOf(LoginViewModel.LoginUiState.StateSuccess::class.java)
        //Checking the state in uiState
        assertThat((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSuccess).type is LoginViewModel.LoginUiStateType.StateRequestAuthToken).isTrue()
        //Checking the error type in the uiState
        if ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSuccess).type is LoginViewModel.LoginUiStateType.StateRequestAuthToken) {
            assertThat(
                ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSuccess).type as LoginViewModel.LoginUiStateType.StateRequestAuthToken).response?.equals(
                    successResponse
                )
            ).isTrue()
        }
        unmockkStatic(FirebaseCrashlytics::class)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request oAuth login api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.validateLogin(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        loginViewModel.requestLogin(
            authToken = "",
            loginType = LoginType.Google("test@gmail.com", "test"),
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        assertThat(statesList).hasSize(3)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSError::class.java)
        //Checking the exception
        assertThat(
            (loginViewModel.uiState.value is LoginViewModel.LoginUiState.StateSError)
                    && (loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).exception is NoNetworkException
        ).isTrue()
        //Checking the error type in the uiState
        assertThat((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestLogin).isTrue()

    }

    @Test
    fun `verify request oAuth login api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<RegisterAuthTokenResponseModel?>()
        coEvery { apiServices.validateLogin(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        loginViewModel.requestLogin(
            authToken = "",
            loginType = LoginType.Google("test@gmail.com", "test"),
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(statesList).hasSize(3)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSError::class.java)
        //Checking the state in uiState
        assertThat((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestLogin).isTrue()
        //Checking the error type in the uiState
        if ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestLogin) {
            assertThat(
                ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type as LoginViewModel.LoginUiStateType.StateRequestLogin).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }


    @Test
    fun `verify request oAuth login api error case with empty data`() = runTest {
        val errorResponseModel = SuccessResponseModel<RegisterAuthTokenResponseModel?>()
        coEvery { apiServices.validateLogin(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        loginViewModel.requestLogin(
            authToken = "",
            loginType = LoginType.Google("test@gmail.com", "test"),
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(statesList).hasSize(3)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSError::class.java)
        //Checking the state in uiState
        assertThat((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestLogin).isTrue()
        //Checking the error type in the uiState
        if ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestLogin) {
            assertThat(
                ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type as LoginViewModel.LoginUiStateType.StateRequestLogin).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }


    @Test
    fun `verify request oAuth login api success case with account not registered`() = runTest {
        val errorResponseModel =
            ErrorResponseModel<RegisterAuthTokenResponseModel?>(
                statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
                status = ApiConstants.STATUS.USER_NOT_FOUND
            )
        coEvery { apiServices.validateLogin(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        loginViewModel.requestLogin(
            authToken = "",
            loginType = LoginType.Google("test@gmail.com", "test"),
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(statesList).hasSize(3)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSError::class.java)
        //Checking the state in uiState
        assertThat((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateAccountNotRegistered).isTrue()
        //Checking the error type in the uiState
        if ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type is LoginViewModel.LoginUiStateType.StateRequestLogin) {
            assertThat(
                ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSError).type as LoginViewModel.LoginUiStateType.StateRequestLogin).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }


    @Test
    fun `verify request oAuth login api success case`() = runTest {
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)
        val successResponse = SuccessResponseModel<LoginResponseModel?>(
            LoginResponseModel(
                accessToken = "test",
                refreshToken = "test", guardianEmail = ""
            ),
        )
        coEvery { apiServices.validateLogin(any()) } returns SuccessResponseModel(
            RegisterAuthTokenResponseModel(
                authCode = "test",
                url = "test",
                userId = "test", onboardingStatus = "", guardianEmailId = ""
            )
        )

        coEvery { apiServices.requestOAuthToken(any()) } returns successResponse

        //Collecting the ui states
        val statesList = mutableListOf<LoginViewModel.LoginUiState>()
        loginViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        loginViewModel.requestLogin(
            authToken = "",
            loginType = LoginType.Google("test@gmail.com", ""),
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        println("login states : $statesList")
        //Check the list size and values inside it.
        assertThat(statesList).hasSize(5)
        assertThat(statesList[0]).isInstanceOf(LoginViewModel.LoginUiState.StateNone::class.java)
        assertThat(statesList[1]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[2]).isInstanceOf(LoginViewModel.LoginUiState.StateSuccess::class.java)
        assertThat(statesList[3]).isInstanceOf(LoginViewModel.LoginUiState.StateLoading::class.java)
        assertThat(statesList[4]).isInstanceOf(LoginViewModel.LoginUiState.StateSuccess::class.java)
        //Checking the state in uiState
        assertThat((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSuccess).type is LoginViewModel.LoginUiStateType.StateRequestAuthToken).isTrue()
        //Checking the error type in the uiState
        if ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSuccess).type is LoginViewModel.LoginUiStateType.StateRequestAuthToken) {
            assertThat(
                ((loginViewModel.uiState.value as LoginViewModel.LoginUiState.StateSuccess).type as LoginViewModel.LoginUiStateType.StateRequestAuthToken).response?.equals(
                    successResponse
                )
            ).isTrue()
        }
        unmockkStatic(FirebaseCrashlytics::class)
    }
}