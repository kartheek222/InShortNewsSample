package com.onexp.remag.registration.domain.usecase

import android.content.Context
import android.provider.Settings
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.onexp.remag.MainDispatcherRule
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.GetTokenKeyForHeaderUseCase
import com.onexp.remag.domain.utils.NetworkUtils
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.RequestInterceptor
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
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

class SuggestInterestUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantRule = InstantTaskExecutorRule()

    private lateinit var mockedContext: Context
    private lateinit var webServer: MockWebServer
    private lateinit var suggestInterestUseCase: SuggestInterestUseCase
    private lateinit var apiServices: ApiServices
    private val gson: Gson = GsonBuilder().create()

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceid"

        webServer = MockWebServer()
        apiServices = mockk<ApiServices>()
        suggestInterestUseCase = SuggestInterestUseCase(mockedContext, apiServices)
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
        suggestInterestUseCase = SuggestInterestUseCase(mockedContext, apiServices)

        val response = suggestInterestUseCase("", "")
        Truth.assertThat(response is SuggestInterestUseCase.SuggestInterestUseCaseResult.StateError)
            .isTrue()
        unmockkObject(NetworkUtils)
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.suggestInterests(any()) } returns ErrorResponseModel(
            data = null
        )
        val testResult = suggestInterestUseCase("", "")
        Truth.assertThat(testResult is SuggestInterestUseCase.SuggestInterestUseCaseResult.StateError)
            .isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        val mockResponse = MockResponse()
        mockResponse.setBody(
            gson.toJson(
                BaseResponseModel<JsonObject>(
                    status = ApiConstants.STATUS.IAT,
                    data = null,
                    statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
                    message = null
                )
            )
        )
        webServer.enqueue(mockResponse)
        val testResult = suggestInterestUseCase("", "")
        Truth.assertThat(testResult is SuggestInterestUseCase.SuggestInterestUseCaseResult.StateSessionTimeout)
            .isFalse()
    }

    @Test
    fun `validate success`() = runTest {
        coEvery { apiServices.suggestInterests(any()) } returns SuccessResponseModel(
            data = JsonObject()
        )
        val testResult = suggestInterestUseCase("", "")
        Truth.assertThat(testResult is SuggestInterestUseCase.SuggestInterestUseCaseResult.StateSuccess)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
        //Shutting down the server  after each test case.
        webServer.shutdown()
    }
}