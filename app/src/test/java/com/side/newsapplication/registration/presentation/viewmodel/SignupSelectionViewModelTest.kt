package com.onexp.remag.registration.presentation.viewmodel

import android.content.Context
import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SessionTimeoutResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.EmailAvailabilityResponseModel
import com.onexp.remag.registration.domain.usecase.EmailValidationUseCase
import com.onexp.remag.repository.network.ApiServices
import io.mockk.coEvery
import io.mockk.mockk
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

@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class SignupSelectionViewModelTest {

    private lateinit var mockedContext: Context
    private lateinit var viewModel: SignupSelectionViewModel
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        apiServices = mockk<ApiServices>()
        viewModel = SignupSelectionViewModel((EmailValidationUseCase(apiServices)))
    }


    @Test
    fun `'validate email with error cases'`() {
        var result = viewModel.isValidEmail(null)
        Truth.assertThat(result).isFalse()

        result = viewModel.isValidEmail("")
        Truth.assertThat(result).isFalse()

        result = viewModel.isValidEmail("test")
        Truth.assertThat(result).isFalse()

        result = viewModel.isValidEmail("test.com")
        Truth.assertThat(result).isFalse()

        result = viewModel.isValidEmail("test@domain")
        Truth.assertThat(result).isFalse()

        result = viewModel.isValidEmail("@domain.com")
        Truth.assertThat(result).isFalse()

        result = viewModel.isValidEmail("test.com")
        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `'validate email with success cases'`() {
        val result = viewModel.isValidEmail("test@domain.com")
        Truth.assertThat(result).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request validate email api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.emailAvailability(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<SignupSelectionViewModel.EmailAvailabilityUiState>()
        viewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Making the API request
        viewModel.requestEmailAvailability("test@domain.com")
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateError::class.java)
        //Checking the exception
        Truth.assertThat(
            (viewModel.uiState.value is SignupSelectionViewModel.EmailAvailabilityUiState.StateError)
                    && (viewModel.uiState.value as SignupSelectionViewModel.EmailAvailabilityUiState.StateError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `verify request validate email api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<EmailAvailabilityResponseModel?>()
        coEvery { apiServices.emailAvailability(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<SignupSelectionViewModel.EmailAvailabilityUiState>()
        viewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        //Making the API request
        viewModel.requestEmailAvailability("test@domain.com")
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateError::class.java)
        //Checking the error type in the uiState
        Truth.assertThat(
            ((viewModel.uiState.value as SignupSelectionViewModel.EmailAvailabilityUiState.StateError).response)?.equals(
                errorResponseModel
            )
        ).isTrue()
    }


    @Test
    fun `verify request validate email api with timout case`() = runTest {
        val errorResponseModel = SessionTimeoutResponseModel<EmailAvailabilityResponseModel?>()
        coEvery { apiServices.emailAvailability(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<SignupSelectionViewModel.EmailAvailabilityUiState>()
        viewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        //Making the API request
        viewModel.requestEmailAvailability("test@domain.com")
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateSessionTimeOut::class.java)
        //Checking the error type in the uiState
        Truth.assertThat(
            ((viewModel.uiState.value as SignupSelectionViewModel.EmailAvailabilityUiState.StateSessionTimeOut).response).equals(
                errorResponseModel
            )
        ).isTrue()
    }


    @Test
    fun `verify request validate email api with empty data`() = runTest {
        val responseModel = SuccessResponseModel<EmailAvailabilityResponseModel?>()
        coEvery { apiServices.emailAvailability(any()) } returns responseModel

        //Collecting the ui states
        val statesList = mutableListOf<SignupSelectionViewModel.EmailAvailabilityUiState>()
        viewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        //Making the API request
        viewModel.requestEmailAvailability("test@domain.com")
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateError::class.java)
        //Checking the error type in the uiState
        Truth.assertThat(
            ((viewModel.uiState.value as SignupSelectionViewModel.EmailAvailabilityUiState.StateError).response)?.equals(
                responseModel
            )
        ).isTrue()
    }


    @Test
    fun `verify request validate email api with success case`() = runTest {
        val responseModel = SuccessResponseModel<EmailAvailabilityResponseModel?>(
            data = EmailAvailabilityResponseModel(
                email = "test@domain.com",
                isAvailable = true
            )
        )
        coEvery { apiServices.emailAvailability(any()) } returns responseModel

        //Collecting the ui states
        val statesList = mutableListOf<SignupSelectionViewModel.EmailAvailabilityUiState>()
        viewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        //Making the API request
        viewModel.requestEmailAvailability("test@domain.com")
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(SignupSelectionViewModel.EmailAvailabilityUiState.StateSuccess::class.java)
        //Checking the error type in the uiState
        Truth.assertThat(
            ((viewModel.uiState.value as SignupSelectionViewModel.EmailAvailabilityUiState.StateSuccess).response).equals(
                responseModel
            )
        ).isTrue()
    }

}