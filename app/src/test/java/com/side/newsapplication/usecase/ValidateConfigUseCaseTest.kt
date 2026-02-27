package com.side.newsapplication.usecase

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.CheckConfigResponseModel
import com.onexp.remag.registration.domain.usecase.ValidateConfigUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Created by Sonu.Sinha  on 2/2/2024
 */
class ValidateConfigUseCaseTest {
    private lateinit var apiService: ApiServices
    private lateinit var systemUnderTest: ValidateConfigUseCase

    @BeforeEach
    fun setUp() {
        apiService = mockk<ApiServices>()
        systemUnderTest = ValidateConfigUseCase(apiService)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiService.validateAppVersion(any()) } throws NoNetworkException()
        val testResult = systemUnderTest.requestValidateAppConfig()
        Truth.assertThat(
            testResult is ValidateConfigUseCase.ValidateConfigUseCaseResult.StateSError && testResult.exception is NoNetworkException
        )
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiService.validateAppVersion(any()) } returns
                BaseResponseModel<CheckConfigResponseModel>(
                    data = null,
                    status = ApiConstants.RESPONSE_FAIL,
                    message = "Unable to process",
                    statusCode = 400
                )
        val testResult = systemUnderTest.requestValidateAppConfig()
        Truth.assertThat(testResult is ValidateConfigUseCase.ValidateConfigUseCaseResult.StateSError && testResult.response!!.status == ApiConstants.RESPONSE_FAIL)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiService.validateAppVersion(any()) } returns BaseResponseModel<CheckConfigResponseModel>(
            data = null,
            status = ApiConstants.RESPONSE_SUCCESS,
            message = "success response",
            statusCode = ApiConstants.RESPONSE_SUCCESS_CODE
        )
        val testResult = systemUnderTest.requestValidateAppConfig()
        Truth.assertThat(
            testResult is ValidateConfigUseCase.ValidateConfigUseCaseResult.StateSuccess && testResult.response.statusCode ==
                    ApiConstants.RESPONSE_SUCCESS_CODE
        )
    }

    @Test
    fun `validate session time out response`() = runTest {
        coEvery { apiService.validateAppVersion(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.RESPONSE_FAIL,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = systemUnderTest.requestValidateAppConfig()
        Truth.assertThat(testResult is ValidateConfigUseCase.ValidateConfigUseCaseResult.StateSessionTimeout && testResult.response!!.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}