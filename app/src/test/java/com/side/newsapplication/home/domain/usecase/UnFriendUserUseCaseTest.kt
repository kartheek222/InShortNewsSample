package com.onexp.remag.home.domain.usecase

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.domain.UnFriendUserUseCase
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

class UnFriendUserUseCaseTest {

    private lateinit var useCase: UnFriendUserUseCase
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        useCase = UnFriendUserUseCase(apiServices)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.unFriendUser(any()) } throws NoNetworkException()
        val testResult: UnFriendUserUseCase.UnFriendRequestCaseResult = useCase.invoke("")
        Truth.assertThat(testResult is UnFriendUserUseCase.UnFriendRequestCaseResult.StateError && testResult.exception is NoNetworkException)
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.unFriendUser(any()) } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_FAIL,
            data = null,
            statusCode = 400,
            message = "Unable to process"
        )
        val testResult = useCase("")
        Truth.assertThat(
            (testResult is UnFriendUserUseCase.UnFriendRequestCaseResult.StateError && testResult.response?.status == ApiConstants.RESPONSE_FAIL)
        ).isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.unFriendUser(any()) } returns BaseResponseModel(
            status = ApiConstants.STATUS.IAT,
            data = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            message = null
        )
        val testResult = useCase("")
        Truth.assertThat(testResult is UnFriendUserUseCase.UnFriendRequestCaseResult.StateSessionTimeout && testResult.response?.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.unFriendUser(any()) } returns SuccessResponseModel()
        val testResult = useCase.invoke("")
        Truth.assertThat(testResult is UnFriendUserUseCase.UnFriendRequestCaseResult.StateSuccess && testResult.response.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

}