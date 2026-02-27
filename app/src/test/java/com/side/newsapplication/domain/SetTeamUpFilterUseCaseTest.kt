package com.onexp.remag.domain

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.home.data.EndorsementModel
import com.onexp.remag.home.domain.SetTeamUpFilterUseCase
import com.onexp.remag.registration.data.FavouriteGames
import com.onexp.remag.registration.data.InterestListResponseModel
import com.onexp.remag.registration.data.MasterPlatformResponseItem
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

class SetTeamUpFilterUseCaseTest {

    private lateinit var apiService: ApiServices
    private lateinit var systemUnderTest: SetTeamUpFilterUseCase

    @BeforeEach
    fun setUp() {
        apiService = mockk<ApiServices>()
        systemUnderTest = SetTeamUpFilterUseCase(apiService)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery {
            apiService.setTeamUpFilter(any())
        } throws NoNetworkException()
        val testResult =
            systemUnderTest.invoke(
                platformsList = arrayListOf<MasterPlatformResponseItem>(),
                selectedGame = arrayListOf<FavouriteGames>(),
                selectedInterest = arrayListOf<InterestListResponseModel>(),
                openGames = arrayListOf<FavouriteGames>(),
                endorsement = arrayListOf<EndorsementModel>()
            )
        Truth.assertThat(
            testResult is SetTeamUpFilterUseCase.SetTeamUpFilterUseCaseResult.StateSError && testResult.exception is NoNetworkException
        )
            .isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery {
            apiService.setTeamUpFilter(any())
        } returns
                BaseResponseModel<Unit>(
                    data = null,
                    status = ApiConstants.RESPONSE_FAIL,
                    message = "Unable to process",
                    statusCode = 400
                )
        val testResult = systemUnderTest.invoke(
            platformsList = arrayListOf<MasterPlatformResponseItem>(),
            selectedGame = arrayListOf<FavouriteGames>(),
            selectedInterest = arrayListOf<InterestListResponseModel>(),
            openGames = arrayListOf<FavouriteGames>(),
            endorsement = arrayListOf<EndorsementModel>()
        )
        Truth.assertThat(testResult is SetTeamUpFilterUseCase.SetTeamUpFilterUseCaseResult.StateSError && testResult.response!!.status == ApiConstants.RESPONSE_FAIL)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery {
            apiService.setTeamUpFilter(any())
        } returns BaseResponseModel<Unit>(
            data = null,
            status = ApiConstants.RESPONSE_SUCCESS,
            message = "success response",
            statusCode = ApiConstants.RESPONSE_SUCCESS_CODE
        )
        val testResult = systemUnderTest.invoke(
            platformsList = arrayListOf<MasterPlatformResponseItem>(),
            selectedGame = arrayListOf<FavouriteGames>(),
            selectedInterest = arrayListOf<InterestListResponseModel>(),
            openGames = arrayListOf<FavouriteGames>(),
            endorsement = arrayListOf<EndorsementModel>()
        )
        Truth.assertThat(
            testResult is SetTeamUpFilterUseCase.SetTeamUpFilterUseCaseResult.StateSuccess && testResult.response.statusCode ==
                    ApiConstants.RESPONSE_SUCCESS_CODE
        )
    }

    @Test
    fun `validate session time out response`() = runTest {
        coEvery {
            apiService.setTeamUpFilter(any())
        } returns BaseResponseModel(
            data = null,
            status = ApiConstants.RESPONSE_FAIL,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = systemUnderTest.invoke(
            platformsList = arrayListOf<MasterPlatformResponseItem>(),
            selectedGame = arrayListOf<FavouriteGames>(),
            selectedInterest = arrayListOf<InterestListResponseModel>(),
            openGames = arrayListOf<FavouriteGames>(),
            endorsement = arrayListOf<EndorsementModel>()
        )
        Truth.assertThat(testResult is SetTeamUpFilterUseCase.SetTeamUpFilterUseCaseResult.StateSessionTimeout && testResult.response!!.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}