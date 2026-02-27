package com.onexp.remag.registration.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SessionTimeoutResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.domain.utils.PKCEUtil
import com.onexp.remag.registration.data.LoginResponseModel
import com.onexp.remag.registration.data.LoginType
import com.onexp.remag.registration.data.RegisterAuthTokenResponseModel
import com.onexp.remag.registration.domain.usecase.RequestGendersUseCase
import com.onexp.remag.registration.domain.usecase.RequestOAuthTokenUseCase
import com.onexp.remag.registration.domain.usecase.SaveLoginPreferencesUseCase
import com.onexp.remag.registration.domain.usecase.SignUpUseCase
import com.onexp.remag.registration.domain.usecase.UserAgeValidationUseCase
import com.onexp.remag.repository.network.ApiServiceBuilder
import com.onexp.remag.repository.network.ApiServices
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
import java.util.Calendar

@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class DobSelectionViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var mockedContext: Context
    private lateinit var viewmodel: DobSelectionViewModel
    private lateinit var apiServices: ApiServices
    private lateinit var preferencesManager: BasePreferencesManager

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        apiServices = mockk<ApiServices>()
        mockkObject(PKCEUtil)
        every { PKCEUtil.generateCodeVerifier() } returns "fakeCodeVerifier"
        every { PKCEUtil.generateCodeChallenge(any()) } returns "fakeCodeVerifier"

        val saveLoginPreferencesUseCase = mockk<SaveLoginPreferencesUseCase>()
        every {
            saveLoginPreferencesUseCase.invoke(
                any(),
                any(),
                any(),
                any()
            )
        } returns Unit


        preferencesManager = mockk(relaxed = true)
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceid"

        mockkStatic(FirebaseMessaging::class)
        every { FirebaseMessaging.getInstance() } returns mockk(relaxed = true)
        mockkObject(ApiServiceBuilder)
        every {
            ApiServiceBuilder.getApiServicesWithUrl(any(), any())
        } returns apiServices

        viewmodel =
            DobSelectionViewModel(
                preferences = preferencesManager,
                requestGendersUseCase = RequestGendersUseCase(apiServices = apiServices),
                signUpUseCase = SignUpUseCase(
                    context = mockedContext,
                    apiServices = apiServices
                ).apply { preferences = preferencesManager },
                saveLoginPreferencesUseCase = saveLoginPreferencesUseCase,
                requestOAuthTokenUseCase = RequestOAuthTokenUseCase(
                    context = mockedContext
                ),
                userAgeValidationUseCase = UserAgeValidationUseCase()
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `validate fields without selecting date of birth`() {
        val result = viewmodel.validateFields(false)
        Truth.assertThat(result)
            .isInstanceOf(DobSelectionViewModel.DobValidationResult.EmptyDOB::class.java)
    }

    @Test
    fun `validate fields dob having the current date`() {
        viewmodel.userDob = Calendar.getInstance()
        val result = viewmodel.validateFields(false)
        Truth.assertThat(result)
            .isInstanceOf(DobSelectionViewModel.DobValidationResult.AgeBelow13::class.java)
    }

    @Test
    fun `validate fields dob having age below 13`() {
        viewmodel.userDob = Calendar.getInstance().apply {
            this[Calendar.YEAR] = this[Calendar.YEAR] - 11
        }
        val result = viewmodel.validateFields(false)
        Truth.assertThat(result)
            .isInstanceOf(DobSelectionViewModel.DobValidationResult.AgeBelow13::class.java)
    }

    @Test
    fun `validate fields without selecting gender`() {
        viewmodel.userDob = Calendar.getInstance().apply {
            this[Calendar.YEAR] = this[Calendar.YEAR] - 20
        }
        val result = viewmodel.validateFields(false)
        Truth.assertThat(result)
            .isInstanceOf(DobSelectionViewModel.DobValidationResult.EmptyGender::class.java)
    }

    @Test
    fun `validate fields without accepting the terms`() {
        viewmodel.userDob = Calendar.getInstance().apply {
            this[Calendar.YEAR] = this[Calendar.YEAR] - 20
        }
        viewmodel.selectedGenderPosition = 1
        viewmodel.signUpType = LoginType.Discord("df")
        val result = viewmodel.validateFields(false)
        Truth.assertThat(result)
            .isInstanceOf(DobSelectionViewModel.DobValidationResult.TermsNotAccepted::class.java)
    }

    @Test
    fun `validate fields with success case`() {
        viewmodel.userDob = Calendar.getInstance().apply {
            this[Calendar.YEAR] = this[Calendar.YEAR] - 20
        }
        viewmodel.selectedGenderPosition = 1
        val result = viewmodel.validateFields(true)
        Truth.assertThat(result)
            .isInstanceOf(DobSelectionViewModel.DobValidationResult.Success::class.java)
    }

    @Test
    fun `verify request genders api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.requestGendersList() } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<DobSelectionViewModel.DobSelectionUiState>()
        viewmodel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Making the API request
        viewmodel.requestGendersList()
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateSuccess::class.java)
    }


    @Test
    fun `verify request genders api with error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.requestGendersList() } returns ErrorResponseModel()

        //Collecting the ui states
        val statesList = mutableListOf<DobSelectionViewModel.DobSelectionUiState>()
        viewmodel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Making the API request
        viewmodel.requestGendersList()
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateSuccess::class.java)
    }

    @Test
    fun `verify request genders api with success case`() = runTest {
        //Mocking the response
        coEvery { apiServices.requestGendersList() } returns SuccessResponseModel(
            data = listOf("Male", "Female")
        )

        //Collecting the ui states
        val statesList = mutableListOf<DobSelectionViewModel.DobSelectionUiState>()
        viewmodel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Making the API request
        viewmodel.requestGendersList()
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateSuccess::class.java)
    }

    @Test
    fun `verify request signup api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.postSignup(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<DobSelectionViewModel.DobSelectionUiState>()
        viewmodel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        preFillTheData(viewmodel)
        //Making the API request
        viewmodel.requestSignupUser(true)
        //Wait until the response is executed.
        advanceUntilIdle()
        println("ui states list = $statesList")
        println("ui states  = ${viewmodel.uiState.value}")
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateError::class.java)
        //Checking the exception
        Truth.assertThat(
            (viewmodel.uiState.value is DobSelectionViewModel.DobSelectionUiState.StateError)
                    && (viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateError).exception is NoNetworkException
        ).isTrue()
    }


    @Test
    fun `verify request signup api with error case`() = runTest {
        //Mocking the response
        val responseModel = ErrorResponseModel<RegisterAuthTokenResponseModel?>()
        coEvery { apiServices.postSignup(any()) } returns responseModel

        //Collecting the ui states
        val statesList = mutableListOf<DobSelectionViewModel.DobSelectionUiState>()
        viewmodel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        preFillTheData(viewmodel)
        //Making the API request
        viewmodel.requestSignupUser(true)
        //Wait until the response is executed.
        advanceUntilIdle()
        println("ui states list = $statesList")
        println("ui states  = ${viewmodel.uiState.value}")
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateError::class.java)
        //Checking the exception
        Truth.assertThat(
            (viewmodel.uiState.value is DobSelectionViewModel.DobSelectionUiState.StateError)
                    && ((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateError).type is DobSelectionViewModel.DobSelectionUiStateType.StateRequestSignup)
        ).isTrue()
    }

    @Test
    fun `verify request signup api with session timeout case`() = runTest {
        //Mocking the response
        val responseModel = SessionTimeoutResponseModel<RegisterAuthTokenResponseModel?>()
        coEvery { apiServices.postSignup(any()) } returns responseModel

        //Collecting the ui states
        val statesList = mutableListOf<DobSelectionViewModel.DobSelectionUiState>()
        viewmodel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        preFillTheData(viewmodel)
        //Making the API request
        viewmodel.requestSignupUser(true)
        //Wait until the response is executed.
        advanceUntilIdle()
        println("ui states list = $statesList")
        println("ui states  = ${viewmodel.uiState.value}")
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateError::class.java)
        //Checking the exception
        Truth.assertThat(
            (viewmodel.uiState.value is DobSelectionViewModel.DobSelectionUiState.StateError)
                    && ((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateError).type is DobSelectionViewModel.DobSelectionUiStateType.StateRequestSignup)
        ).isTrue()
    }

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
        val statesList = mutableListOf<DobSelectionViewModel.DobSelectionUiState>()
        viewmodel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        preFillTheData(viewmodel)
        //Making the API request
        viewmodel.requestSignupUser(true)
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(5)
        Truth.assertThat(statesList[0])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateSuccess::class.java)
        Truth.assertThat(statesList[3])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[4])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateSuccess::class.java)
        //Checking the state in uiState
        Truth.assertThat((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateSuccess).type is DobSelectionViewModel.DobSelectionUiStateType.StateRequestOAuthSignup)
            .isTrue()
        //Checking the error type in the uiState
        if ((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateSuccess).type is DobSelectionViewModel.DobSelectionUiStateType.StateRequestOAuthSignup) {
            Truth.assertThat(
                ((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateSuccess).type as DobSelectionViewModel.DobSelectionUiStateType.StateRequestOAuthSignup).response?.equals(
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
        val statesList = mutableListOf<DobSelectionViewModel.DobSelectionUiState>()
        viewmodel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        preFillTheData(viewmodel)
        //Making the API request
        viewmodel.requestOAuthToken(
            RegisterAuthTokenResponseModel(
                authCode = "Test",
                onboardingStatus = "Test",
                url = "Test",
                userId = "Test",
                profilePic = " Test",
                gamerId = "Test", guardianEmailId = ""
            )
        )

        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateError::class.java)
        Truth.assertThat(
            (viewmodel.uiState.value is DobSelectionViewModel.DobSelectionUiState.StateError)
                    && (viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateError).exception is NoNetworkException
        ).isTrue()

    }

    //6). Request OAuth Token api failure case
    @Test
    fun `verify request  oAuthToken api failure case`() = runTest {
        val errorResponse = ErrorResponseModel<LoginResponseModel?>()
        coEvery { apiServices.requestOAuthToken(any()) } returns errorResponse

        //Collecting the ui states
        val statesList = mutableListOf<DobSelectionViewModel.DobSelectionUiState>()
        viewmodel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        preFillTheData(viewmodel)
        //Making the API request
        viewmodel.requestOAuthToken(
            RegisterAuthTokenResponseModel(
                authCode = "Test",
                onboardingStatus = "Test",
                url = "Test",
                userId = "Test",
                profilePic = " Test",
                gamerId = "Test", guardianEmailId = ""
            )
        )

        //Wait until the response is executed.
        advanceUntilIdle()
        println("ui states list = $statesList")
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateError::class.java)
        //Checking the state in uiState
        Truth.assertThat((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateError).type is DobSelectionViewModel.DobSelectionUiStateType.StateRequestOAuthSignup)
            .isTrue()
        //Checking the error type in the uiState
        if ((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateError).type is DobSelectionViewModel.DobSelectionUiStateType.StateRequestOAuthSignup) {
            Truth.assertThat(
                ((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateError).type as DobSelectionViewModel.DobSelectionUiStateType.StateRequestOAuthSignup).response?.equals(
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
        val statesList = mutableListOf<DobSelectionViewModel.DobSelectionUiState>()
        viewmodel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        preFillTheData(viewmodel)
        //Making the API request
        viewmodel.requestOAuthToken(
            RegisterAuthTokenResponseModel(
                authCode = "Test",
                onboardingStatus = "Test",
                url = "Test",
                userId = "Test",
                profilePic = " Test",
                gamerId = "Test", guardianEmailId = ""
            )
        )

        //Wait until the response is executed.
        advanceUntilIdle()
        println("uiStates = $statesList")
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(DobSelectionViewModel.DobSelectionUiState.StateError::class.java)
        //Checking the state in uiState
        Truth.assertThat((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateError).type is DobSelectionViewModel.DobSelectionUiStateType.StateRequestOAuthSignup)
            .isTrue()
        //Checking the error type in the uiState
        if ((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateError).type is DobSelectionViewModel.DobSelectionUiStateType.StateRequestOAuthSignup) {
            Truth.assertThat(
                ((viewmodel.uiState.value as DobSelectionViewModel.DobSelectionUiState.StateError).type as DobSelectionViewModel.DobSelectionUiStateType.StateRequestOAuthSignup).response?.equals(
                    errorResponseModel
                )
            ).isTrue()
        }
    }


    private fun preFillTheData(viewmodel: DobSelectionViewModel) {
        viewmodel.gendersList = mutableListOf("test", "test")
        viewmodel.selectedGenderPosition = 0
        viewmodel.signUpType = LoginType.Discord("er")
        viewmodel.oAuthToken = "test"
    }

}