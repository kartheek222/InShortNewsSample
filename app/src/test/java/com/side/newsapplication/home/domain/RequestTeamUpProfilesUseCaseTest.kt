package com.side.newsapplication.home.domain

import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.side.newsapplication.data.ErrorResponseModel
import com.side.newsapplication.data.SessionTimeoutResponseModel
import com.side.newsapplication.data.SuccessResponseModel
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
class RequestTeamUpProfilesUseCaseTest {

    private lateinit var apiServices: ApiServices
    private lateinit var requestTeamUpProfilesUseCase: RequestTeamUpProfilesUseCase

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        requestTeamUpProfilesUseCase = RequestTeamUpProfilesUseCase(apiServices)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }


    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.requestTeamUpProfiles() } throws NoNetworkException()

        val response = requestTeamUpProfilesUseCase()
        println("network error test response = $response")
        Truth.assertThat(
            (response is RequestTeamUpProfilesUseCase.RequestProfilesCaseResult.StateError)
                    && (response.exception is NoNetworkException)
        ).isTrue()
    }


    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.requestTeamUpProfiles() } returns ErrorResponseModel()

        val response = requestTeamUpProfilesUseCase()
        println("network error test response = $response")
        Truth.assertThat(response is RequestTeamUpProfilesUseCase.RequestProfilesCaseResult.StateError)
            .isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.requestTeamUpProfiles() } returns SessionTimeoutResponseModel()

        val response = requestTeamUpProfilesUseCase()
        println("network error test response = $response")
        Truth.assertThat(response is RequestTeamUpProfilesUseCase.RequestProfilesCaseResult.StateSessionTimeout)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.requestTeamUpProfiles() } returns SuccessResponseModel()

        val response = requestTeamUpProfilesUseCase()
        println("network error test response = $response")
        Truth.assertThat(response is RequestTeamUpProfilesUseCase.RequestProfilesCaseResult.StateSuccess)
            .isTrue()
    }

}