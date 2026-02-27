package com.side.newsapplication.home.domain.usecase

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.domain.UpdateFriendRequestUseCase
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

class UpdateFriendRequestUseCaseTest {

    private lateinit var useCase: UpdateFriendRequestUseCase
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        useCase = UpdateFriendRequestUseCase(apiServices)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.updateFriendRequest(any()) } throws NoNetworkException()
        val testResult: UpdateFriendRequestUseCase.UpdateFriendRequestCaseResult =
            useCase.invoke("", true)
        Truth.assertThat(testResult is UpdateFriendRequestUseCase.UpdateFriendRequestCaseResult.StateError && testResult.exception is NoNetworkException)
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.updateFriendRequest(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.RESPONSE_FAIL,
            message = "Unable to process",
            statusCode = 400
        )
        val testResult = useCase.invoke("", true)
        Truth.assertThat(testResult is UpdateFriendRequestUseCase.UpdateFriendRequestCaseResult.StateError && testResult.response?.status == ApiConstants.RESPONSE_FAIL)
            .isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.updateFriendRequest(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.STATUS.IAT,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = useCase.invoke("", true)
        Truth.assertThat(testResult is UpdateFriendRequestUseCase.UpdateFriendRequestCaseResult.StateSessionTimeout && testResult.response?.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.updateFriendRequest(any()) } returns SuccessResponseModel()
        val testResult = useCase.invoke("", true)
        Truth.assertThat(testResult is UpdateFriendRequestUseCase.UpdateFriendRequestCaseResult.StateSuccess && testResult.response.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

}