package com.onexp.remag.registration.domain.usecase

import android.content.Context
import android.provider.Settings
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.onexp.remag.MainDispatcherRule
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.domain.GetTokenKeyForHeaderUseCase
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.domain.utils.NetworkUtils
import com.onexp.remag.registration.data.LoginType
import com.onexp.remag.registration.data.RegisterAuthTokenResponseModel
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.RequestInterceptor
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by kartheek.sabbisetty on 30-01-2024
 */
class RequestLoginUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantRule = InstantTaskExecutorRule()

    private lateinit var mockedContext: Context
    private lateinit var webServer: MockWebServer
    private lateinit var requestLoginUseCase: RequestLoginUseCase
    private lateinit var apiServices: ApiServices
    private val gson: Gson = GsonBuilder().create()

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceid"

        webServer = MockWebServer()
        apiServices =
            Retrofit.Builder().baseUrl(webServer.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiServices::class.java)
        requestLoginUseCase = RequestLoginUseCase(mockedContext, apiServices)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
        //Shutting down the server  after each test case.
        webServer.shutdown()
    }

    @Test
    fun `validate network error`() = runTest {
        mockkObject(NetworkUtils)
        every { NetworkUtils.isConnected(any()) } returns false

        val client = OkHttpClient.Builder()
            .addInterceptor(RequestInterceptor(mockedContext, GetTokenKeyForHeaderUseCase()))
            .build()

        apiServices =
            Retrofit.Builder()
                .baseUrl(webServer.url("/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiServices::class.java)
        requestLoginUseCase = RequestLoginUseCase(mockedContext, apiServices)

        val response = requestLoginUseCase(
            userName = "",
            password = "",
            loginType = LoginType.Email,
            authToken = "",
            codeVerifier = ""
        )
        println("network error test response = $response")
        assertThat(
            (response is RequestLoginUseCase.LoginUseCaseResult.StateSError)
                    && (response.exception is NoNetworkException)
        ).isTrue()

        unmockkObject(NetworkUtils)
    }

    @Test
    fun `validate error response`() = runTest {
        val mockResponse = MockResponse()
        mockResponse.setBody(
            gson.toJson(
                BaseResponseModel<String>(
                    status = ApiConstants.RESPONSE_FAIL,
                    data = null,
                    statusCode = 201,
                    message = null
                )
            )
        )
        webServer.enqueue(mockResponse)

        val response = requestLoginUseCase("", "", LoginType.Email, "", "")
        println("network error test response = $response")
        assertThat(
            response is RequestLoginUseCase.LoginUseCaseResult.StateSError
                    && response.response!!.statusCode == 201
        ).isTrue()
    }


    @Test
    fun `validate session timeout error response`() = runTest {
        val mockResponse = MockResponse()
        mockResponse.setBody(
            gson.toJson(
                BaseResponseModel<String>(
                    status = ApiConstants.STATUS.IAT,
                    data = null,
                    statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
                    message = null
                )
            )
        )
        webServer.enqueue(mockResponse)

        val response = requestLoginUseCase("", "", LoginType.Email, "", "")
        println("network error test response = $response")
        assertThat(response is RequestLoginUseCase.LoginUseCaseResult.StateSessionTimeout).isTrue()
    }


    @Test
    fun `validate error response with null auth code`() = runTest {
        val mockResponse = MockResponse()
        mockResponse.setBody(
            gson.toJson(
                BaseResponseModel(
                    status = ApiConstants.RESPONSE_SUCCESS,
                    data = RegisterAuthTokenResponseModel(
                        authCode = null,
                        url = null,
                        userId = null,
                        onboardingStatus = "", guardianEmailId = ""
                    ),
                    statusCode = ApiConstants.RESPONSE_SUCCESS_CODE,
                    message = null
                )
            )
        )
        webServer.enqueue(mockResponse)

        val response = requestLoginUseCase("", "", LoginType.Email, "", "")
        println("network error test response = $response")
        assertThat(response is RequestLoginUseCase.LoginUseCaseResult.StateSError).isTrue()
    }


    @Test
    fun `validate success response`() = runTest {
        val mockResponse = MockResponse()
        mockResponse.setBody(
            gson.toJson(
                BaseResponseModel(
                    status = ApiConstants.RESPONSE_SUCCESS,
                    data = RegisterAuthTokenResponseModel(
                        authCode = "test",
                        url = "test",
                        userId = "test",
                        onboardingStatus = "", guardianEmailId = ""
                    ),
                    statusCode = ApiConstants.RESPONSE_SUCCESS_CODE,
                    message = null
                )
            )
        )
        webServer.enqueue(mockResponse)

        val response = requestLoginUseCase("", "", LoginType.Email, "", "")
        println("network error test response = $response")
        assertThat(response is RequestLoginUseCase.LoginUseCaseResult.StateSuccess).isTrue()
    }
}