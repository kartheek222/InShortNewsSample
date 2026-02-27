package com.onexp.remag.registration.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.domain.utils.PKCEUtil
import com.onexp.remag.registration.data.LoginResponseModel
import com.onexp.remag.registration.data.RegisterAuthTokenResponseModel
import com.onexp.remag.registration.data.RequestSignUpModel
import com.onexp.remag.registration.domain.usecase.EmailValidationUseCase
import com.onexp.remag.registration.domain.usecase.PasswordValidationUseCase
import com.onexp.remag.registration.domain.usecase.RequestOAuthTokenUseCase
import com.onexp.remag.registration.domain.usecase.SignUpUseCase
import com.onexp.remag.registration.domain.usecase.UserAgeValidationUseCase
import com.onexp.remag.repository.network.ApiServiceBuilder
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.preferences.BasePreferencesManager
import com.onexp.remag.repository.preferences.PreferencesManager
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
import java.util.Calendar


/**
 * Created by kartheek.sabbisetty on 31-01-2024
 */
@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class SignUpViewModelTest {
    private lateinit var mockedContext: Context
    private lateinit var signUpViewModel: SignUpViewModel
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

        signUpViewModel = SignUpViewModel(
            emailValidationUseCase = EmailValidationUseCase(apiServices),
            signUpUseCase = SignUpUseCase(mockedContext, apiServices).apply {
                preferences = mockk<PreferencesManager>(relaxed = true)
            },
            requestOAuthTokenUseCase = RequestOAuthTokenUseCase(mockedContext),
            agaValidationUseCase = UserAgeValidationUseCase(),
            passwordValidationUseCase = PasswordValidationUseCase(),
            generalPreferences = preferencesManager,
            encryptedPreferences = preferencesManager
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Settings.Secure::class)
        unmockkStatic(FirebaseMessaging::class)
        unmockkObject(PKCEUtil)
    }

    @Test
    fun `validate email error`() = runTest {
        var result = signUpViewModel.emailValidate(null)
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.ErrorType &&
                    result.error is EmailValidationUseCase.EmailErrorType.EmailEmptyError
        ).isTrue()

        result = signUpViewModel.emailValidate("")
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.ErrorType &&
                    result.error is EmailValidationUseCase.EmailErrorType.EmailEmptyError
        ).isTrue()

        result = signUpViewModel.emailValidate(" ")
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.ErrorType &&
                    result.error is EmailValidationUseCase.EmailErrorType.EmailEmptyError
        ).isTrue()

        result = signUpViewModel.emailValidate("test")
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.ErrorType &&
                    result.error is EmailValidationUseCase.EmailErrorType.EmailIncorrect
        ).isTrue()
    }

    @Test
    fun `validate email success`() = runTest {
        val result = signUpViewModel.emailValidate("test@gmail.com")
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.Success
        ).isTrue()
    }


    @Test
    fun `validate password error`() = runTest {
        var result = signUpViewModel.passwordValidate(null)
        Truth.assertThat(
            result is PasswordValidationUseCase.PasswordUseCaseResult.Error
                    && result.errorType is PasswordValidationUseCase.PasswordErrorType.Empty
        ).isTrue()

        result = signUpViewModel.passwordValidate("")
        Truth.assertThat(
            result is PasswordValidationUseCase.PasswordUseCaseResult.Error &&
                    result.errorType is PasswordValidationUseCase.PasswordErrorType.Empty
        )
            .isTrue()

        result = signUpViewModel.passwordValidate(" ")
        Truth.assertThat(
            result is PasswordValidationUseCase.PasswordUseCaseResult.Error &&
                    result.errorType is PasswordValidationUseCase.PasswordErrorType.Empty
        ).isTrue()

        result = signUpViewModel.passwordValidate("test1234")
        Truth.assertThat(
            result is PasswordValidationUseCase.PasswordUseCaseResult.Error &&
                    result.errorType is PasswordValidationUseCase.PasswordErrorType.UpperCaseError
        ).isTrue()

        result = signUpViewModel.passwordValidate("TEST@1234")
        Truth.assertThat(
            result is PasswordValidationUseCase.PasswordUseCaseResult.Error &&
                    result.errorType is PasswordValidationUseCase.PasswordErrorType.LowerCaseError
        ).isTrue()

        result = signUpViewModel.passwordValidate("Testtest")
        Truth.assertThat(
            result is PasswordValidationUseCase.PasswordUseCaseResult.Error &&
                    result.errorType is PasswordValidationUseCase.PasswordErrorType.NumbersError
        ).isTrue()

        result = signUpViewModel.passwordValidate("Test1234")
        Truth.assertThat(
            result is PasswordValidationUseCase.PasswordUseCaseResult.Error &&
                    result.errorType is PasswordValidationUseCase.PasswordErrorType.SpecialCharacterError
        ).isTrue()

        result = signUpViewModel.passwordValidate("Test@12")
        Truth.assertThat(
            result is PasswordValidationUseCase.PasswordUseCaseResult.Error &&
                    result.errorType is PasswordValidationUseCase.PasswordErrorType.LengthError
        ).isTrue()
    }

    @Test
    fun `validate password success`() = runTest {
        val result = signUpViewModel.passwordValidate("Test@1234")
        Truth.assertThat(
            result is PasswordValidationUseCase.PasswordUseCaseResult.Success
        ).isTrue()
    }

    @Test
    fun `validate dob error`() = runTest {
        var result = signUpViewModel.dOBEntryCheck("")
        Truth.assertThat(result).isTrue()

        result = signUpViewModel.dOBEntryCheck(" ")
        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `validate dob age error`() = runTest {
        val calendar: Calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 11)
        calendar.set(Calendar.YEAR, 2023)

        val result = signUpViewModel.isValidDob(calendar)
        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `validate dob age success`() = runTest {
        val calendar: Calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.YEAR, 2003)

        val result = signUpViewModel.isValidDob(calendar)
        Truth.assertThat(result).isTrue()
    }


    //1). No Network test case Signup API
    @Test
    fun `verify request signup api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.postSignup(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<SignUpViewModel.SignUpUiState>()
        signUpViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        signUpViewModel.signUp(
            RequestSignUpModel(
                "fakeUserPassword",
                "fakeUserPassword",
                "fakeUserOAuthToken",
                "fakeUserDOB",
                "fakeEmail",
                "fakeDeviceCode",
                false,
                "fakeCode",
                "fakeApiId",
            )
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateError::class.java)
        //Checking the exception
        Truth.assertThat(
            (signUpViewModel.uiState.value is SignUpViewModel.SignUpUiState.StateError)
                    && (signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).exception is NoNetworkException
        ).isTrue()
    }

    //2). Signup API Error
    @Test
    fun `verify request signup api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<RegisterAuthTokenResponseModel?>()
        coEvery { apiServices.postSignup(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<SignUpViewModel.SignUpUiState>()
        signUpViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        signUpViewModel.signUp(
            RequestSignUpModel(
                "fakeUserPassword",
                "fakeUserPassword",
                "fakeUserOAuthToken",
                "fakeUserDOB",
                "fakeEmail",
                "fakeDeviceCode",
                false,
                "fakeCode",
                "fakeApiId",
            )
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateError::class.java)
        //Checking the exception
        Truth.assertThat((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type is SignUpViewModel.SignUpUiStateType.StateRequestSignup)
            .isTrue()
        //Checking the error type in the uiState
        if ((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type is SignUpViewModel.SignUpUiStateType.StateRequestSignup) {
            Truth.assertThat(
                ((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type as SignUpViewModel.SignUpUiStateType.StateRequestSignup).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }

    //3). Signup API Success but in response get the empty data
    @Test
    fun `verify request signup api error case with empty data`() = runTest {
        val errorResponseModel = SuccessResponseModel<RegisterAuthTokenResponseModel?>()
        coEvery { apiServices.postSignup(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<SignUpViewModel.SignUpUiState>()
        signUpViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        signUpViewModel.signUp(
            RequestSignUpModel(
                "fakeUserPassword",
                "fakeUserPassword",
                "fakeUserOAuthToken",
                "fakeUserDOB",
                "fakeEmail",
                "fakeDeviceCode",
                false,
                "fakeCode",
                "fakeApiId",
            )
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateError::class.java)
        //Checking the state in uiState
        Truth.assertThat((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type is SignUpViewModel.SignUpUiStateType.StateRequestSignup)
            .isTrue()
        //Checking the error type in the uiState
        if ((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type is SignUpViewModel.SignUpUiStateType.StateRequestSignup) {
            Truth.assertThat(
                ((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type as SignUpViewModel.SignUpUiStateType.StateRequestSignup).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }

    //4). Signup API Success and get the success response object
    @Test
    fun `validate success response`() = runTest {
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)
        val successResponseModel =
            SuccessResponseModel<RegisterAuthTokenResponseModel?>(
                RegisterAuthTokenResponseModel(
                    authCode = "authCode",
                    url = "test",
                    userId = "testId",
                    onboardingStatus = "testStatus",
                    profilePic = "testPic",
                    gamerId = "testId", guardianEmailId = ""
                )
            )
        val successResponse = SuccessResponseModel<LoginResponseModel?>(
            LoginResponseModel(
                accessToken = "test",
                refreshToken = "test", guardianEmail = ""
            ),
        )
        coEvery { apiServices.postSignup(any()) } returns successResponseModel
        coEvery { apiServices.requestOAuthToken(any()) } returns successResponse

        //Collecting the ui states
        val statesList = mutableListOf<SignUpViewModel.SignUpUiState>()
        signUpViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Making the API request
        signUpViewModel.signUp(
            RequestSignUpModel(
                "fakeUserPassword",
                "fakeUserPassword",
                "fakeUserOAuthToken",
                "fakeUserDOB",
                "fakeEmail",
                "fakeDeviceCode",
                false,
                "fakeCode",
                "fakeApiId",
            )
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(5)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateSuccess::class.java)
        Truth.assertThat(statesList[3])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateLoading::class.java)
        Truth.assertThat(statesList[4])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateSuccess::class.java)
        //Checking the state in uiState
        Truth.assertThat((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateSuccess).type is SignUpViewModel.SignUpUiStateType.StateRequestOAuthSignup)
            .isTrue()
        //Checking the error type in the uiState
        if ((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateSuccess).type is SignUpViewModel.SignUpUiStateType.StateRequestOAuthSignup) {
            Truth.assertThat(
                ((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateSuccess).type as SignUpViewModel.SignUpUiStateType.StateRequestOAuthSignup).response?.equals(
                    successResponse
                )
            ).isTrue()
        }
        unmockkStatic(FirebaseCrashlytics::class)
    }

    //5). Request OAuth Token Network test case
    @Test
    fun `verify request oAuth signup api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.requestOAuthToken(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<SignUpViewModel.SignUpUiState>()
        signUpViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        signUpViewModel.requestOAuthToken(
            RegisterAuthTokenResponseModel(
                authCode = "",
                gamerId = "",
                profilePic = "",
                userId = "",
                onboardingStatus = "",
                url = "", guardianEmailId = ""
            )
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateError::class.java)
        Truth.assertThat(
            (signUpViewModel.uiState.value is SignUpViewModel.SignUpUiState.StateError)
                    && (signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).exception is NoNetworkException
        ).isTrue()

    }

    //6). Request OAuth Token api failure case
    @Test
    fun `verify request  oAuthToken api failure case`() = runTest {
        val errorResponse = ErrorResponseModel<LoginResponseModel?>()
        coEvery { apiServices.requestOAuthToken(any()) } returns errorResponse

        //Collecting the ui states
        val statesList = mutableListOf<SignUpViewModel.SignUpUiState>()
        signUpViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        signUpViewModel.requestOAuthToken(
            RegisterAuthTokenResponseModel(
                userId = "",
                authCode = "",
                url = "",
                onboardingStatus = "", guardianEmailId = ""
            )
        )
        //Wait until the response is executed.
        advanceUntilIdle()
        println("ui states list = $statesList")
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateError::class.java)
        //Checking the state in uiState
        Truth.assertThat((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type is SignUpViewModel.SignUpUiStateType.StateRequestOAuthSignup)
            .isTrue()
        //Checking the error type in the uiState
        if ((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type is SignUpViewModel.SignUpUiStateType.StateRequestOAuthSignup) {
            Truth.assertThat(
                ((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type as SignUpViewModel.SignUpUiStateType.StateRequestOAuthSignup).response?.equals(
                    errorResponse
                )
            ).isTrue()
        }
    }

    //7). Request OAuth Token api failure case
    @Test
    fun `verify request oAuth Token api error case with empty data`() = runTest {
        val errorResponseModel = SuccessResponseModel<LoginResponseModel?>()
        coEvery { apiServices.requestOAuthToken(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<SignUpViewModel.SignUpUiState>()
        signUpViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        signUpViewModel.requestOAuthToken(
            RegisterAuthTokenResponseModel(
                userId = "",
                authCode = "",
                url = "",
                onboardingStatus = "", guardianEmailId = ""
            )
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignUpViewModel.SignUpUiState.StateError::class.java)
        //Checking the state in uiState
        Truth.assertThat((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type is SignUpViewModel.SignUpUiStateType.StateRequestOAuthSignup)
            .isTrue()
        //Checking the error type in the uiState
        if ((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type is SignUpViewModel.SignUpUiStateType.StateRequestOAuthSignup) {
            Truth.assertThat(
                ((signUpViewModel.uiState.value as SignUpViewModel.SignUpUiState.StateError).type as SignUpViewModel.SignUpUiStateType.StateRequestOAuthSignup).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }
}