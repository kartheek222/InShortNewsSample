package com.onexp.remag.registration.domain.usecase

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.domain.EditUserFavouriteGameUseCase
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
 * Created by ismail.akhtar  on 3/4/2024
 */
class EditUserFavouriteGameUseCaseTest {

    lateinit var apiServices: ApiServices
    lateinit var sut: EditUserFavouriteGameUseCase
    lateinit var gameResponseItem: FavouriteGames

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        sut = EditUserFavouriteGameUseCase(apiServices)
        gameResponseItem = FavouriteGames("test", "test", false, 11)

    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.editUserFavouriteGames(any()) } throws NoNetworkException()
        val testResult: EditUserFavouriteGameUseCase.SetFavouriteGameUseCaseResult =
            sut.invoke(listOf(gameResponseItem), emptyList())
        Truth.assertThat(testResult is EditUserFavouriteGameUseCase.SetFavouriteGameUseCaseResult.StateSError && testResult.exception is NoNetworkException)
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.editUserFavouriteGames(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.RESPONSE_FAIL,
            message = "Unable to process",
            statusCode = 400
        )
        val testResult = sut.invoke(listOf(gameResponseItem), emptyList())
        Truth.assertThat(
            testResult is EditUserFavouriteGameUseCase.SetFavouriteGameUseCaseResult.StateSError && testResult.response?.status!! == ApiConstants.RESPONSE_FAIL
        )
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.editUserFavouriteGames(any()) } returns SuccessResponseModel(
            data = Unit
        )
        val testResult = sut.invoke(listOf(gameResponseItem), emptyList())
        Truth.assertThat(testResult is EditUserFavouriteGameUseCase.SetFavouriteGameUseCaseResult.StateSuccess && testResult.response?.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @Test
    fun `validate session time out exception`() = runTest {
        coEvery { apiServices.editUserFavouriteGames(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.STATUS.IAT,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = sut.invoke(listOf(gameResponseItem), emptyList())
        Truth.assertThat(testResult is EditUserFavouriteGameUseCase.SetFavouriteGameUseCaseResult.StateSessionTimeout && testResult.response!!.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}