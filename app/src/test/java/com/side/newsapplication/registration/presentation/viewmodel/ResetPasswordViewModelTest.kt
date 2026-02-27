@file:OptIn(ExperimentalCoroutinesApi::class)

package com.onexp.remag.registration.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ERROR_MESSAGE_SESSION_EXPIRED
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.domain.ClearLoginPreferencesUseCase
import com.onexp.remag.registration.domain.usecase.PasswordValidationUseCase
import com.onexp.remag.registration.domain.usecase.ResetPasswordUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import com.onexp.remag.repository.preferences.BasePreferencesManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ExtendWith(MainDispatcherExtension::class)
class ResetPasswordViewModelTest {

    @RelaxedMockK
    private lateinit var mockedContext: Context

    @MockK
    private lateinit var apiServices: ApiServices

    @RelaxedMockK
    private lateinit var preferencesManager: BasePreferencesManager
    private lateinit var passwordValidationUseCase: PasswordValidationUseCase
    private lateinit var resetPasswordViewModel: ResetPasswordViewModel

    @BeforeEach
    fun setUp() {
        //When it is executed all annotated properties are substituted with corresponding objects
        MockKAnnotations.init(this)
        //Mock Settings class for getting device id
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceid"

        //initializing password validation use case dependency
        passwordValidationUseCase = PasswordValidationUseCase()
        //initializing view model
        resetPasswordViewModel = ResetPasswordViewModel(
            ResetPasswordUseCase(mockedContext, apiServices),
            passwordValidationUseCase,
            ClearLoginPreferencesUseCase(mockedContext, preferencesManager, preferencesManager)
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Settings.Secure::class)
    }

    @ParameterizedTest(name = "{index}: {0} and {1} is equal - {2}")
    @MethodSource("passwordMatchArguments")
    fun `unequal password and retype password returns false`(
        password: String,
        confirmPass: String,
        expected: Boolean
    ) {
        val result = resetPasswordViewModel.isNewPassAndConfirmPassMatch(password, confirmPass)
        assertEquals(result, expected)
    }

    @Test
    fun `validate old password field`() {
        //when old password is empty
        var result = resetPasswordViewModel.validateOldPasswordField(
            oldPassword = "",
        )
        assertThat(result).isInstanceOf(ResetPasswordViewModel.ValidationErrorType.EmptyOldPassword::class.java)
        result = resetPasswordViewModel.validateOldPasswordField(
            oldPassword = "    ",
        )
        assertThat(result).isInstanceOf(ResetPasswordViewModel.ValidationErrorType.EmptyOldPassword::class.java)
    }

    @Test
    fun `validate new password field`() {
        //when new password is empty
        var result = resetPasswordViewModel.validateNewPasswordField(
            newPassword = ""
        )
        assertThat(result).isInstanceOf(ResetPasswordViewModel.ValidationErrorType.EmptyNewPassword::class.java)

        //when new password does not meet its length criteria
        result = resetPasswordViewModel.validateNewPasswordField(
            newPassword = "Pole@12"
        )
        assertThat(
            (result is ResetPasswordViewModel.ValidationErrorType.PasswordValidation) &&
                    (result.passwordError is PasswordValidationUseCase.PasswordErrorType.LengthError)
        ).isTrue()

        //when new password does not contain at least one uppercase symbol
        result = resetPasswordViewModel.validateNewPasswordField(
            newPassword = "pole@123"
        )
        assertThat(
            (result is ResetPasswordViewModel.ValidationErrorType.PasswordValidation) &&
                    (result.passwordError is PasswordValidationUseCase.PasswordErrorType.UpperCaseError)
        ).isTrue()

        //when new password does not contain at least one lower case symbol
        result = resetPasswordViewModel.validateNewPasswordField(
            newPassword = "POLE@123"
        )
        assertThat(
            (result is ResetPasswordViewModel.ValidationErrorType.PasswordValidation) &&
                    (result.passwordError is PasswordValidationUseCase.PasswordErrorType.LowerCaseError)
        ).isTrue()

        //when new password does not contain at least one digit
        result = resetPasswordViewModel.validateNewPasswordField(
            newPassword = "Pole@pole"
        )
        assertThat(
            (result is ResetPasswordViewModel.ValidationErrorType.PasswordValidation) &&
                    (result.passwordError is PasswordValidationUseCase.PasswordErrorType.NumbersError)
        ).isTrue()

        //when new password does not contain any special character which has given in criteria
        result = resetPasswordViewModel.validateNewPasswordField(
            newPassword = "Pole+123"
        )
        assertThat(
            (result is ResetPasswordViewModel.ValidationErrorType.PasswordValidation) &&
                    (result.passwordError is PasswordValidationUseCase.PasswordErrorType.SpecialCharacterError)
        ).isTrue()

        //when new password does not contain any error
        result = resetPasswordViewModel.validateNewPasswordField(
            newPassword = "Pole_123",
        )
        assertThat(result).isInstanceOf(ResetPasswordViewModel.ValidationErrorType.None::class.java)
    }

    @Test
    fun `validate confirm password field`() {
        //when confirm password is empty
        var result = resetPasswordViewModel.validateConfirmPasswordField(
            newPassword = "Pole@123",
            confirmPassword = ""
        )
        assertThat(result).isInstanceOf(ResetPasswordViewModel.ValidationErrorType.EmptyConfirmPassword::class.java)

        //when new password and confirm password does not match its content
        result = resetPasswordViewModel.validateConfirmPasswordField(
            newPassword = "Pole@123",
            confirmPassword = "Pole@124"
        )
        assertThat(result).isInstanceOf(ResetPasswordViewModel.ValidationErrorType.ConfirmPasswordMismatch::class.java)
    }

    @Test
    fun `verify reset password api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.resetPassword(any()) } throws NoNetworkException()

        //containing respective states list while execution of api
        val uiStatesList = mutableListOf<ResetPasswordViewModel.ResetPasswordUiState>()
        //Collecting the ui states
        resetPasswordViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api request
        resetPasswordViewModel.resetPassword("fakePassword", "fakePassword")

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(uiStatesList).hasSize(3)
        assertThat(uiStatesList[0]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.None::class.java)
        assertThat(uiStatesList[1]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Loading::class.java)
        assertThat(uiStatesList[2]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Error::class.java)

        //Checking the exception
        assertThat(
            resetPasswordViewModel.uiState.value is ResetPasswordViewModel.ResetPasswordUiState.Error &&
                    (resetPasswordViewModel.uiState.value as ResetPasswordViewModel.ResetPasswordUiState.Error).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `verify reset password api with error case`() = runTest {
        //Mocking the fake response
        val errorResponseModel = ErrorResponseModel<Any>()
        coEvery { apiServices.resetPassword(any()) } returns errorResponseModel

        //it will contain states list
        val uiStatesList = mutableListOf<ResetPasswordViewModel.ResetPasswordUiState>()
        //collecting ui states
        resetPasswordViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api request
        resetPasswordViewModel.resetPassword("fakePassword", "fakePassword")

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(uiStatesList).hasSize(3)
        assertThat(uiStatesList[0]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.None::class.java)
        assertThat(uiStatesList[1]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Loading::class.java)
        assertThat(uiStatesList[2]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Error::class.java)

        //Checking the error in uiState
        assertThat(
            (resetPasswordViewModel.uiState.value as ResetPasswordViewModel.ResetPasswordUiState.Error).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify reset password api with session time out error case`() = runTest {
        //Mocking the fake response
        val errorResponseModel = ErrorResponseModel<Any>(
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            status = ApiConstants.STATUS.IAT,
            message = ERROR_MESSAGE_SESSION_EXPIRED
        )
        coEvery { apiServices.resetPassword(any()) } returns errorResponseModel

        //it will contain states list
        val uiStatesList = mutableListOf<ResetPasswordViewModel.ResetPasswordUiState>()
        //collecting ui states
        resetPasswordViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api request
        resetPasswordViewModel.resetPassword("fakePassword", "fakePassword")

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(uiStatesList).hasSize(3)
        assertThat(uiStatesList[0]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.None::class.java)
        assertThat(uiStatesList[1]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Loading::class.java)
        assertThat(uiStatesList[2]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.StateSSessionTimeOut::class.java)

        //Checking the error in uiState
        assertThat(
            (resetPasswordViewModel.uiState.value as ResetPasswordViewModel.ResetPasswordUiState.StateSSessionTimeOut).response?.equals(
                errorResponseModel
            )
        ).isTrue()
        //checking the status code
        assertThat(
            (resetPasswordViewModel.uiState.value as ResetPasswordViewModel.ResetPasswordUiState.StateSSessionTimeOut).response?.statusCode ==
                    ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        ).isTrue()
    }

    @Test
    fun `verify reset password api with success case`() = runTest {
        //Mocking the fake response
        val successResponse = SuccessResponseModel<Any>()
        coEvery { apiServices.resetPassword(any()) } returns successResponse

        //it will contain states list
        val uiStatesList = mutableListOf<ResetPasswordViewModel.ResetPasswordUiState>()

        //Collecting the ui state
        resetPasswordViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api
        resetPasswordViewModel.resetPassword("fakePassword", "fakePassword")

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(uiStatesList).hasSize(3)
        assertThat(uiStatesList[0]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.None::class.java)
        assertThat(uiStatesList[1]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Loading::class.java)
        assertThat(uiStatesList[2]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Success::class.java)

        //Checking the success state
        assertThat(
            resetPasswordViewModel.uiState.value is ResetPasswordViewModel.ResetPasswordUiState.Success &&
                    (resetPasswordViewModel.uiState.value as ResetPasswordViewModel.ResetPasswordUiState.Success).response?.equals(
                        successResponse
                    ) == true
        ).isTrue()
    }

    @Test
    fun `verify forgot password api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.resetPasswordIfForgotOldPassword(any()) } throws NoNetworkException()

        //containing respective states list while execution of api
        val uiStatesList = mutableListOf<ResetPasswordViewModel.ResetPasswordUiState>()
        //Collecting the ui states
        resetPasswordViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api request
        resetPasswordViewModel.resetPasswordIfForgotOldPassword("", "", "fakePassword", "", "")

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(uiStatesList).hasSize(3)
        assertThat(uiStatesList[0]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.None::class.java)
        assertThat(uiStatesList[1]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Loading::class.java)
        assertThat(uiStatesList[2]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Error::class.java)

        //Checking the exception
        assertThat(
            resetPasswordViewModel.uiState.value is ResetPasswordViewModel.ResetPasswordUiState.Error &&
                    (resetPasswordViewModel.uiState.value as ResetPasswordViewModel.ResetPasswordUiState.Error).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `verify forgot password api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>()
        //Mocking the response
        coEvery { apiServices.resetPasswordIfForgotOldPassword(any()) } returns errorResponseModel

        //containing respective states list while execution of api
        val uiStatesList = mutableListOf<ResetPasswordViewModel.ResetPasswordUiState>()
        //Collecting the ui states
        resetPasswordViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api request
        resetPasswordViewModel.resetPasswordIfForgotOldPassword("", "", "fakePassword", "", "")

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(uiStatesList).hasSize(3)
        assertThat(uiStatesList[0]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.None::class.java)
        assertThat(uiStatesList[1]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Loading::class.java)
        assertThat(uiStatesList[2]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Error::class.java)

        //Checking the exception
        assertThat(
            (resetPasswordViewModel.uiState.value as ResetPasswordViewModel.ResetPasswordUiState.Error).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `verify forgot password api with session time out error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>(
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            status = ApiConstants.STATUS.IAT,
            message = ERROR_MESSAGE_SESSION_EXPIRED
        )
        //Mocking the response
        coEvery { apiServices.resetPasswordIfForgotOldPassword(any()) } returns errorResponseModel

        //containing respective states list while execution of api
        val uiStatesList = mutableListOf<ResetPasswordViewModel.ResetPasswordUiState>()
        //Collecting the ui states
        resetPasswordViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api request
        resetPasswordViewModel.resetPasswordIfForgotOldPassword("", "", "fakePassword", "", "")

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(uiStatesList).hasSize(3)
        assertThat(uiStatesList[0]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.None::class.java)
        assertThat(uiStatesList[1]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Loading::class.java)
        assertThat(uiStatesList[2]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.StateSSessionTimeOut::class.java)

        //Checking the exception
        assertThat(
            (resetPasswordViewModel.uiState.value as ResetPasswordViewModel.ResetPasswordUiState.StateSSessionTimeOut).response?.equals(
                errorResponseModel
            )
        ).isTrue()
        //checking the status code
        assertThat(
            (resetPasswordViewModel.uiState.value as ResetPasswordViewModel.ResetPasswordUiState.StateSSessionTimeOut).response?.statusCode ==
                    ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        ).isTrue()
    }

    @Test
    fun `verify forgot password api with success case`() = runTest {
        val successResponse = SuccessResponseModel<Any>()
        //Mocking the response
        coEvery { apiServices.resetPasswordIfForgotOldPassword(any()) } returns successResponse

        //containing respective states list while execution of api
        val uiStatesList = mutableListOf<ResetPasswordViewModel.ResetPasswordUiState>()
        //Collecting the ui states
        resetPasswordViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api request
        resetPasswordViewModel.resetPasswordIfForgotOldPassword("", "", "fakePassword", "", "")

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        assertThat(uiStatesList).hasSize(3)
        assertThat(uiStatesList[0]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.None::class.java)
        assertThat(uiStatesList[1]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Loading::class.java)
        assertThat(uiStatesList[2]).isInstanceOf(ResetPasswordViewModel.ResetPasswordUiState.Success::class.java)

        //Checking the response
        assertThat(
            resetPasswordViewModel.uiState.value is ResetPasswordViewModel.ResetPasswordUiState.Success &&
                    (resetPasswordViewModel.uiState.value as ResetPasswordViewModel.ResetPasswordUiState.Success).response?.equals(
                        successResponse
                    ) == true
        ).isTrue()
    }

    companion object {
        @JvmStatic
        fun passwordMatchArguments(): Stream<Arguments> = Stream.of(
            Arguments.of("Pole@123", "abc@123", false),
            Arguments.of("Pole@123", "pole@123", false),
            Arguments.of("Pole@123", "POLE@123", false),
            Arguments.of("", "", false),
            Arguments.of(" ", "  ", false),
            Arguments.of("Pole@123", "Pole@123", true)
        )
    }
}