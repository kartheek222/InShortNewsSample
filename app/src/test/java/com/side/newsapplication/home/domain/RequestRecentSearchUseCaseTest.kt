package com.side.newsapplication.home.domain

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.domain.utils.NetworkUtils
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RequestRecentSearchUseCaseTest {

    private lateinit var useCase: RequestRecentSearchUseCase
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        useCase = RequestRecentSearchUseCase(apiServices)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.getRecentSearches(any()) } throws NoNetworkException()
        val testResult = useCase("")
        Truth.assertThat(
            testResult is RequestRecentSearchUseCase.RequestRecentSearchUseCaseResult.StateSError && testResult.exception is NoNetworkException
        ).isTrue()
        unmockkObject(NetworkUtils)
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.getRecentSearches(any()) } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_FAIL,
            data = null,
            statusCode = 400,
            message = "Unable to process"
        )
        val testResult = useCase("")
        Truth.assertThat(
            (testResult is RequestRecentSearchUseCase.RequestRecentSearchUseCaseResult.StateSError && testResult.response?.status == ApiConstants.RESPONSE_FAIL)
        ).isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.getRecentSearches(any()) } returns BaseResponseModel(
            status = ApiConstants.STATUS.IAT,
            data = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            message = null
        )
        val testResult = useCase("")
        Truth.assertThat(testResult is RequestRecentSearchUseCase.RequestRecentSearchUseCaseResult.StateSessionTimeout && testResult.response?.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.getRecentSearches(any()) } returns SuccessResponseModel()
        val testResult = useCase("")
        Truth.assertThat(testResult is RequestRecentSearchUseCase.RequestRecentSearchUseCaseResult.StateSuccess && testResult.response?.status == ApiConstants.RESPONSE_SUCCESS)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}