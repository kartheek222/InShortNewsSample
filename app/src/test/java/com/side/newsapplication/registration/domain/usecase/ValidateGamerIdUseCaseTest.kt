package com.side.newsapplication.registration.domain.usecase

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.GamerIdSelectionResponse
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Created by Sonu.Sinha  on 2/1/2024
 */
class ValidateGamerIdUseCaseTest {

    lateinit var apiServices: ApiServices
    lateinit var sut: ValidateGamerIdUseCase

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        sut = ValidateGamerIdUseCase(apiServices)

    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.validateGamerId(any()) } throws NoNetworkException()
        val testResult: ValidateGamerIdUseCase.ValidateGamerIdUseCaseResult = sut.invoke("")
        Truth.assertThat(testResult is ValidateGamerIdUseCase.ValidateGamerIdUseCaseResult.StateSError && testResult.exception is NoNetworkException)
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.validateGamerId(any()) } returns BaseResponseModel<GamerIdSelectionResponse>(
            data = null,
            status = ApiConstants.RESPONSE_FAIL,
            message = "Unable to process",
            statusCode = 400
        )
        val testResult = sut.invoke("")
        Truth.assertThat(
            testResult is ValidateGamerIdUseCase.ValidateGamerIdUseCaseResult.StateSError && testResult.response?.status!! == ApiConstants.RESPONSE_FAIL
        )
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.validateGamerId(any()) } returns SuccessResponseModel(data = GamerIdSelectionResponse("test",true))
        val testResult = sut.invoke("")
        Truth.assertThat(testResult is ValidateGamerIdUseCase.ValidateGamerIdUseCaseResult.StateSuccess && testResult.response.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @Test
    fun `validate session time out exception`() = runTest {
        coEvery { apiServices.validateGamerId(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.STATUS.IAT,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = sut.invoke("")
        Truth.assertThat(testResult is ValidateGamerIdUseCase.ValidateGamerIdUseCaseResult.StateSessionTimeout && testResult.response!!.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @Test
    fun `validate empty deviceId returns statusCode 400`() = runTest {
        coEvery { apiServices.validateGamerId(any()) } returns BaseResponseModel(
            data = null,
            statusCode = 400,
            status = ApiConstants.RESPONSE_FAIL,
            message = "Bad Request"
        )
        val testResult = sut.invoke("")
        Truth.assertThat(testResult is ValidateGamerIdUseCase.ValidateGamerIdUseCaseResult.StateSError && testResult.response!!.statusCode == 400)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}