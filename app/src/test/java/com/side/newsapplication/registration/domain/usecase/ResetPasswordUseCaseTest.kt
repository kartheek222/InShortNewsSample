package com.onexp.remag.registration.domain.usecase

import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class ResetPasswordUseCaseTest {

    @RelaxedMockK
    private lateinit var mockedContext: Context

    @MockK
    private lateinit var apiServices: ApiServices
    private lateinit var resetPasswordUseCase: ResetPasswordUseCase

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceId"
        resetPasswordUseCase = ResetPasswordUseCase(mockedContext, apiServices)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `validate reset password api with network error`() = runTest {
        coEvery { apiServices.resetPassword(any()) } throws NoNetworkException()
        val response = resetPasswordUseCase.resetPassword("fakePassword", "fakePassword")
        assertThat(response).isInstanceOf(ResetPasswordUseCase.ResetPasswordUseCaseResult.StateSError::class.java)
        //checking the exception
        assertThat(
            (response as ResetPasswordUseCase.ResetPasswordUseCaseResult.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate reset password api with session timeout error`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>(
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            status = ApiConstants.STATUS.IAT
        )
        coEvery { apiServices.resetPassword(any()) } returns errorResponseModel
        val response = resetPasswordUseCase.resetPassword("fakePassword", "fakePassword")
        assertThat(response).isInstanceOf(ResetPasswordUseCase.ResetPasswordUseCaseResult.SessionTimeoutError::class.java)
        //checking the exception
        assertThat(
            (response as ResetPasswordUseCase.ResetPasswordUseCaseResult.SessionTimeoutError).value?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate reset password api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>()
        coEvery { apiServices.resetPassword(any()) } returns errorResponseModel
        val response = resetPasswordUseCase.resetPassword("fakePassword", "fakePassword")
        assertThat(response).isInstanceOf(ResetPasswordUseCase.ResetPasswordUseCaseResult.StateSError::class.java)
        //checking the exception
        assertThat(
            (response as ResetPasswordUseCase.ResetPasswordUseCaseResult.StateSError).value?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate reset password api with success`() = runTest {
        val successResponseModel = SuccessResponseModel<Any>()
        coEvery { apiServices.resetPassword(any()) } returns successResponseModel
        val response = resetPasswordUseCase.resetPassword("fakePassword", "fakePassword")
        assertThat(response).isInstanceOf(ResetPasswordUseCase.ResetPasswordUseCaseResult.Success::class.java)
        //checking the exception
        assertThat(
            (response as ResetPasswordUseCase.ResetPasswordUseCaseResult.Success).value?.equals(
                successResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate forgot password api with network error`() = runTest {
        coEvery { apiServices.resetPasswordIfForgotOldPassword(any()) } throws NoNetworkException()
        val response =
            resetPasswordUseCase.resetPasswordIfForgotOldPassword("", "", "fakePassword", "", "")
        assertThat(response).isInstanceOf(ResetPasswordUseCase.ResetPasswordUseCaseResult.StateSError::class.java)
        //checking the exception
        assertThat(
            (response as ResetPasswordUseCase.ResetPasswordUseCaseResult.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate forgot password api with session timeout error`() = runTest {
        val errorResponseModel =
            ErrorResponseModel<Any>(statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE, status = ApiConstants.STATUS.IAT)
        coEvery { apiServices.resetPasswordIfForgotOldPassword(any()) } returns errorResponseModel
        val response =
            resetPasswordUseCase.resetPasswordIfForgotOldPassword("", "", "fakePassword", "", "")
        assertThat(response).isInstanceOf(ResetPasswordUseCase.ResetPasswordUseCaseResult.SessionTimeoutError::class.java)
        //checking the exception
        assertThat(
            (response as ResetPasswordUseCase.ResetPasswordUseCaseResult.SessionTimeoutError).value?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate forgot password api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<Any>()
        coEvery { apiServices.resetPasswordIfForgotOldPassword(any()) } returns errorResponseModel
        val response =
            resetPasswordUseCase.resetPasswordIfForgotOldPassword("", "", "fakePassword", "", "")
        assertThat(response).isInstanceOf(ResetPasswordUseCase.ResetPasswordUseCaseResult.StateSError::class.java)
        //checking the exception
        assertThat(
            (response as ResetPasswordUseCase.ResetPasswordUseCaseResult.StateSError).value?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate forgot password api with success`() = runTest {
        val successResponseModel = SuccessResponseModel<Any>()
        coEvery { apiServices.resetPasswordIfForgotOldPassword(any()) } returns successResponseModel
        val response =
            resetPasswordUseCase.resetPasswordIfForgotOldPassword("", "", "fakePassword", "", "")
        assertThat(response).isInstanceOf(ResetPasswordUseCase.ResetPasswordUseCaseResult.Success::class.java)
        //checking the exception
        assertThat(
            (response as ResetPasswordUseCase.ResetPasswordUseCaseResult.Success).value?.equals(
                successResponseModel
            )
        ).isTrue()
    }
}