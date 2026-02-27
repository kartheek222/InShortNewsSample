package com.onexp.remag.home.domain

import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SessionTimeoutResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.repository.network.ApiServices
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Created by kartheek.sabbisetty on 5/10/2024.
 */
@ExtendWith(MainDispatcherExtension::class)
class RequestTeamUpSwipeUseCaseTest {

    private lateinit var requestTeamUpSwipeUseCase: RequestTeamUpSwipeUseCase
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        requestTeamUpSwipeUseCase = RequestTeamUpSwipeUseCase(apiServices)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.requestUpdateTeamUpSwipe(any()) } throws NoNetworkException()

        val response = requestTeamUpSwipeUseCase("", RequestTeamUpSwipeUseCase.FriendAction.Skip)
        println("network error test response = $response")
        Truth.assertThat(
            (response is RequestTeamUpSwipeUseCase.TeamUpSwipeUseCaseResult.StateError)
                    && (response.exception is NoNetworkException)
        ).isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.requestUpdateTeamUpSwipe(any()) } returns ErrorResponseModel()

        val response = requestTeamUpSwipeUseCase("", RequestTeamUpSwipeUseCase.FriendAction.Skip)
        println("network error test response = $response")
        Truth.assertThat(response is RequestTeamUpSwipeUseCase.TeamUpSwipeUseCaseResult.StateError)
            .isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.requestUpdateTeamUpSwipe(any()) } returns SessionTimeoutResponseModel()

        val response = requestTeamUpSwipeUseCase("", RequestTeamUpSwipeUseCase.FriendAction.Skip)
        println("network error test response = $response")
        Truth.assertThat(response is RequestTeamUpSwipeUseCase.TeamUpSwipeUseCaseResult.StateSessionTimeout)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.requestUpdateTeamUpSwipe(any()) } returns SuccessResponseModel()

        val response = requestTeamUpSwipeUseCase("", RequestTeamUpSwipeUseCase.FriendAction.Skip)
        println("network error test response = $response")
        Truth.assertThat(response is RequestTeamUpSwipeUseCase.TeamUpSwipeUseCaseResult.StateSuccess)
            .isTrue()
    }
}