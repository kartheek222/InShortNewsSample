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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Created by Sonu.Sinha  on 3/28/2024
 */
class SetFavouriteGameUseCaseTest {

    lateinit var apiServices: ApiServices
    lateinit var sut: SetFavouriteGameUseCase
    lateinit var gameResponseItem: FavouriteGames

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        sut = SetFavouriteGameUseCase(apiServices)
        gameResponseItem = FavouriteGames("test", "test", true, 11)

    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.setFavouriteGames(any()) } throws NoNetworkException()
        val testResult: SetFavouriteGameUseCase.SetFavouriteGameUseCaseResult =
            sut.invoke(listOf(gameResponseItem))
        Truth.assertThat(testResult is SetFavouriteGameUseCase.SetFavouriteGameUseCaseResult.StateSError && testResult.exception is NoNetworkException)
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.setFavouriteGames(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.RESPONSE_FAIL,
            message = "Unable to process",
            statusCode = 400
        )
        val testResult = sut.invoke(listOf(gameResponseItem))
        Truth.assertThat(
            testResult is SetFavouriteGameUseCase.SetFavouriteGameUseCaseResult.StateSError && testResult.response?.status!! == ApiConstants.RESPONSE_FAIL
        )
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.setFavouriteGames(any()) } returns SuccessResponseModel(
            data = Unit
        )
        val testResult = sut.invoke(listOf(gameResponseItem))
        Truth.assertThat(testResult is SetFavouriteGameUseCase.SetFavouriteGameUseCaseResult.StateSuccess && testResult.response?.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @Test
    fun `validate session time out exception`() = runTest {
        coEvery { apiServices.setFavouriteGames(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.STATUS.IAT,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = sut.invoke(listOf(gameResponseItem))
        Truth.assertThat(testResult is SetFavouriteGameUseCase.SetFavouriteGameUseCaseResult.StateSessionTimeout && testResult.response!!.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}