package com.side.newsapplication.registration.domain.usecase

import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.LoginResponseModel
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServiceBuilder
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import com.onexp.remag.repository.preferences.BasePreferencesManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Created by kartheek.sabbisetty on 30-01-2024
 */
@ExtendWith(MainDispatcherExtension::class)
class RequestOAuthTokenUseCaseTest {

    private lateinit var mockedContext: Context
    private lateinit var requestTokensUseCase: RequestOAuthTokenUseCase
    private lateinit var apiServices: ApiServices
    private lateinit var preferencesManager: BasePreferencesManager

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        mockkStatic(Settings.Secure::class)

        preferencesManager = mockk(relaxed = true)
        apiServices = mockk<ApiServices>()
        mockkObject(ApiServiceBuilder)
        every {
            ApiServiceBuilder.getApiServicesWithUrl(any(), any())
        } returns apiServices

        requestTokensUseCase = RequestOAuthTokenUseCase(mockedContext)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }


    @Test
    fun `validate network error`() = runTest {

        coEvery { apiServices.requestOAuthToken(any()) } throws NoNetworkException()

        val response = requestTokensUseCase("", "", "", "", "")
        println("network error test response = $response")
        Truth.assertThat(
            (response is RequestOAuthTokenUseCase.RequestOAuthUseCaseResult.StateSError)
                    && (response.exception is NoNetworkException)
        ).isTrue()
    }


    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.requestOAuthToken(any()) } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_FAIL,
            data = null,
            statusCode = 201,
            message = null
        )

        val response = requestTokensUseCase("", "", "", "", "")
        println("network error test response = $response")
        Truth.assertThat(response is RequestOAuthTokenUseCase.RequestOAuthUseCaseResult.StateSError)
            .isTrue()
    }


    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.requestOAuthToken(any()) } returns BaseResponseModel(
            status = ApiConstants.STATUS.IAT,
            data = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            message = null
        )

        val response = requestTokensUseCase("", "", "", "", "")
        println("network error test response = $response")
        Truth.assertThat(response is RequestOAuthTokenUseCase.RequestOAuthUseCaseResult.StateSessionTimeout)
            .isTrue()
    }


    @Test
    fun `validate error response with null auth code`() = runTest {
        coEvery { apiServices.requestOAuthToken(any()) } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_SUCCESS,
            data = null,
            statusCode = ApiConstants.RESPONSE_SUCCESS_CODE,
            message = null
        )
        val response = requestTokensUseCase("", "", "", "", "")
        println("network error test response = $response")
        Truth.assertThat(response is RequestOAuthTokenUseCase.RequestOAuthUseCaseResult.StateSError)
            .isTrue()
    }


    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.requestOAuthToken(any()) } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_SUCCESS,
            data = LoginResponseModel(accessToken = "", refreshToken = "", guardianEmail = ""),
            statusCode = ApiConstants.RESPONSE_SUCCESS_CODE,
            message = null
        )

        val response = requestTokensUseCase("", "", "", "", "")
        println("network error test response = $response")
        Truth.assertThat(response is RequestOAuthTokenUseCase.RequestOAuthUseCaseResult.StateSuccess)
            .isTrue()
    }

}