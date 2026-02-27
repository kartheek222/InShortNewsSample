package com.side.newsapplication.registration.domain.usecase

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.MasterPlatformResponseItem
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
 * Created by Sonu.Sinha  on 3/28/2024
 */
class SetPlatformUseCaseTest {

    lateinit var apiServices: ApiServices
    lateinit var sut: SetPlatformUseCase
    lateinit var masterPlatformResponseItem: MasterPlatformResponseItem

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        sut = SetPlatformUseCase(apiServices)
        masterPlatformResponseItem = MasterPlatformResponseItem("test", "test", 1)

    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.setSelectedPlatform(any()) } throws NoNetworkException()
        val testResult: SetPlatformUseCase.SetPlatformUseCaseResult =
            sut.invoke(listOf(masterPlatformResponseItem))
        Truth.assertThat(testResult is SetPlatformUseCase.SetPlatformUseCaseResult.StateSError && testResult.exception is NoNetworkException)
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.setSelectedPlatform(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.RESPONSE_FAIL,
            message = "Unable to process",
            statusCode = 400
        )
        val testResult = sut.invoke(listOf(masterPlatformResponseItem))
        Truth.assertThat(
            testResult is SetPlatformUseCase.SetPlatformUseCaseResult.StateSError && testResult.response?.status!! == ApiConstants.RESPONSE_FAIL
        )
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.setSelectedPlatform(any()) } returns SuccessResponseModel(
            data = Unit
        )
        val testResult = sut.invoke(listOf(masterPlatformResponseItem))
        Truth.assertThat(testResult is SetPlatformUseCase.SetPlatformUseCaseResult.StateSuccess && testResult.response?.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @Test
    fun `validate session time out exception`() = runTest {
        coEvery { apiServices.setSelectedPlatform(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.STATUS.IAT,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = sut.invoke(listOf(masterPlatformResponseItem))
        Truth.assertThat(testResult is SetPlatformUseCase.SetPlatformUseCaseResult.StateSessionTimeout && testResult.response!!.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}