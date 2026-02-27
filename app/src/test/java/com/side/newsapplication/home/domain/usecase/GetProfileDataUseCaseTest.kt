package com.onexp.remag.home.domain.usecase

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.data.ProfileResponse
import com.onexp.remag.home.domain.GetProfileDataUseCase
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

class GetProfileDataUseCaseTest {

    private lateinit var useCase: GetProfileDataUseCase
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        useCase = GetProfileDataUseCase(apiServices)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.requestProfileData(any()) } throws NoNetworkException()
        val response: GetProfileDataUseCase.GetProfileDataUseCaseResult = useCase.invoke("")
        Truth.assertThat(
            (response is GetProfileDataUseCase.GetProfileDataUseCaseResult.StateSError)
                    && (response.exception is NoNetworkException)
        ).isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.requestProfileData(any()) } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_FAIL,
            data = null,
            statusCode = 400,
            message = "Unable to process"
        )
        val response = useCase("")
        Truth.assertThat(
            response is GetProfileDataUseCase.GetProfileDataUseCaseResult.StateSError && response.response?.status!! == ApiConstants.RESPONSE_FAIL
        ).isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.requestProfileData(any()) } returns BaseResponseModel(
            status = ApiConstants.STATUS.IAT,
            data = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            message = null
        )
        val response = useCase.invoke("")
        Truth.assertThat(response is GetProfileDataUseCase.GetProfileDataUseCaseResult.StateSessionTimeout && response.response!!.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        val profileResponse = mockk<ProfileResponse>()
        coEvery { apiServices.requestProfileData(any()) } returns SuccessResponseModel(data = profileResponse)
        val response = useCase.invoke("")
        Truth.assertThat(response is GetProfileDataUseCase.GetProfileDataUseCaseResult.StateSuccess && response.response?.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}