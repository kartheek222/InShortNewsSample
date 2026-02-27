package com.onexp.remag.registration.domain.usecase

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.FavouriteGames
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
 * Created by Sonu.Sinha  on 3/28/2024
 */
class FavouriteGameUseCaseTest {

    lateinit var apiServices: ApiServices
    lateinit var sut: FavouriteGameUseCase

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        sut = FavouriteGameUseCase(apiServices)

    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.requestFavouriteGames(any()) } throws NoNetworkException()
        val testResult: FavouriteGameUseCase.FavouriteGameUseCaseResult = sut.invoke("test")
        Truth.assertThat(testResult is FavouriteGameUseCase.FavouriteGameUseCaseResult.StateSError && testResult.exception is NoNetworkException)
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.requestFavouriteGames(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.RESPONSE_FAIL,
            message = "Unable to process",
            statusCode = 400
        )
        val testResult = sut.invoke("test")
        Truth.assertThat(
            testResult is FavouriteGameUseCase.FavouriteGameUseCaseResult.StateSError && testResult.response?.status!! == ApiConstants.RESPONSE_FAIL
        )
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.requestFavouriteGames(any()) } returns SuccessResponseModel(
            data = mutableListOf(
                FavouriteGames(
                    "test",
                    "test", false, 111
                )
            )
        )
        val testResult = sut.invoke("test")
        Truth.assertThat(testResult is FavouriteGameUseCase.FavouriteGameUseCaseResult.StateSuccess && testResult.response?.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @Test
    fun `validate session time out exception`() = runTest {
        coEvery { apiServices.requestFavouriteGames(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.STATUS.IAT,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = sut.invoke("test")
        Truth.assertThat(testResult is FavouriteGameUseCase.FavouriteGameUseCaseResult.StateSessionTimeout && testResult.response!!.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}