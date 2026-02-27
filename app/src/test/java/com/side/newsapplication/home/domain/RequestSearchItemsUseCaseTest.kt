package com.onexp.remag.home.domain

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.data.GlobalSearchItems
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class RequestSearchItemsUseCaseTest {

    private lateinit var useCase: RequestSearchItemsUseCase
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        useCase = RequestSearchItemsUseCase(apiServices)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.requestGlobalSearchItems(any()) } throws NoNetworkException()
        val testResult: RequestSearchItemsUseCase.RequestSearchItemsUseCaseResult = useCase("")
        Truth.assertThat(testResult is RequestSearchItemsUseCase.RequestSearchItemsUseCaseResult.StateSError && testResult.exception is NoNetworkException)
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.requestGlobalSearchItems(any()) } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_FAIL,
            data = null,
            statusCode = 400,
            message = "Unable to process"
        )
        val testResult = useCase("")
        Truth.assertThat(
            (testResult is RequestSearchItemsUseCase.RequestSearchItemsUseCaseResult.StateSError && testResult.response?.status == ApiConstants.RESPONSE_FAIL)
        ).isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.requestGlobalSearchItems(any()) } returns BaseResponseModel(
            status = ApiConstants.STATUS.IAT,
            data = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            message = null
        )
        val testResult = useCase("")
        Truth.assertThat(testResult is RequestSearchItemsUseCase.RequestSearchItemsUseCaseResult.StateSessionTimeout && testResult.response?.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        val mockList = mockk<List<GlobalSearchItems>>()
        coEvery { apiServices.requestGlobalSearchItems(any()) } returns SuccessResponseModel(mockList)
        val testResult = useCase("")
        Truth.assertThat(testResult is RequestSearchItemsUseCase.RequestSearchItemsUseCaseResult.StateSuccess && testResult.response?.status == ApiConstants.RESPONSE_SUCCESS)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}