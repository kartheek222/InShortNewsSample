package com.side.newsapplication.domain

import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth
import com.side.newsapplication.data.ErrorResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RequestSignOutUseCaseTest {


    private lateinit var mockedContext: Context
    private lateinit var signOutUseCase: RequestSignOutUseCase
    private lateinit var apiService: ApiServices

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceid"
        apiService = mockk<ApiServices>()
        signOutUseCase = RequestSignOutUseCase(mockedContext, apiService)

    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }


    @Test
    fun `validate network error`() = runTest {
        coEvery { apiService.requestSignOut(any()) } throws NoNetworkException()
        val testResult = signOutUseCase(RequestSignOutUseCase.LogoutType.SPECIFIC)

        Truth.assertThat(
            testResult is RequestSignOutUseCase.UseCaseResult.Error && testResult.exception is NoNetworkException
        ).isTrue()
    }


    @Test
    fun `validate error response`() = runTest {
        coEvery { apiService.requestSignOut(any()) } returns
                ErrorResponseModel()
        val testResult = signOutUseCase(RequestSignOutUseCase.LogoutType.SPECIFIC)
        Truth.assertThat(testResult is RequestSignOutUseCase.UseCaseResult.Error && testResult.responseModel!!.status == ApiConstants.RESPONSE_FAIL)
            .isTrue()
    }


    @Test
    fun `validate success response`() = runTest {
        coEvery { apiService.requestSignOut(any()) } returns SuccessResponseModel()
        val testResult = signOutUseCase(RequestSignOutUseCase.LogoutType.SPECIFIC)
        Truth.assertThat(
            testResult is RequestSignOutUseCase.UseCaseResult.Error && testResult.responseModel!!.statusCode ==
                    ApiConstants.RESPONSE_SUCCESS_CODE
        )
    }
}