package com.onexp.remag.domain

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.home.data.GetTeamUpFilterModel
import com.onexp.remag.home.domain.GetTeamUpFilterUseCase
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

class GetTeamUpFilterUseCaseTest {

    private lateinit var apiService: ApiServices
    private lateinit var systemUnderTest: GetTeamUpFilterUseCase

    @BeforeEach
    fun setUp() {
        apiService = mockk<ApiServices>()
        systemUnderTest = GetTeamUpFilterUseCase(apiService)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery {
            apiService.getTeamUpFilter()
        } throws NoNetworkException()
        val testResult = systemUnderTest.invoke()
        Truth.assertThat(
            testResult is GetTeamUpFilterUseCase.TeamUpFilterUseCaseResult.StateSError && testResult.exception is NoNetworkException
        )
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery {
            apiService.getTeamUpFilter()
        } returns
                BaseResponseModel<GetTeamUpFilterModel?>(
                    data = null,
                    status = ApiConstants.RESPONSE_FAIL,
                    message = "Unable to process",
                    statusCode = 400
                )
        val testResult = systemUnderTest.invoke()
        Truth.assertThat(testResult is GetTeamUpFilterUseCase.TeamUpFilterUseCaseResult.StateSError && testResult.response!!.status == ApiConstants.RESPONSE_FAIL)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery {
            apiService.getTeamUpFilter()
        } returns BaseResponseModel<GetTeamUpFilterModel?>(
            data = null,
            status = ApiConstants.RESPONSE_SUCCESS,
            message = "success response",
            statusCode = ApiConstants.RESPONSE_SUCCESS_CODE
        )
        val testResult = systemUnderTest.invoke()
        Truth.assertThat(
            testResult is GetTeamUpFilterUseCase.TeamUpFilterUseCaseResult.StateSuccess && testResult.response?.statusCode ==
                    ApiConstants.RESPONSE_SUCCESS_CODE
        )
    }

    @Test
    fun `validate session time out response`() = runTest {
        coEvery {
            apiService.getTeamUpFilter()
        } returns BaseResponseModel(
            data = null,
            status = ApiConstants.RESPONSE_FAIL,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = systemUnderTest.invoke()
        Truth.assertThat(testResult is GetTeamUpFilterUseCase.TeamUpFilterUseCaseResult.StateSessionTimeout && testResult.response!!.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}