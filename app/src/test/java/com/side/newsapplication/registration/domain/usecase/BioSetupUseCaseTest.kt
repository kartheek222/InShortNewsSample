package com.onexp.remag.registration.domain.usecase

import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.onexp.remag.MainDispatcherRule
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.domain.GetTokenKeyForHeaderUseCase
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.domain.utils.NetworkUtils
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

class BioSetupUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockedContext: Context
    private lateinit var webServer: MockWebServer
    private lateinit var bioSetupUseCase: BioSetupUseCase
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
        bioSetupUseCase = BioSetupUseCase(mockedContext, apiServices)
    }

    @Test
    fun `given bioInfo is null when validationBioSetup then return BioFailedEmpty`() {
        val result = bioSetupUseCase.validationBioSetup(null)
        Truth.assertThat(
            result is BioSetupUseCase.BioFailedValidation.BioFailedEmpty
        ).isTrue()
    }

    @Test
    fun `given bioInfo is empty when validationBioSetup then return BioFailedEmpty`() {
        val result = bioSetupUseCase.validationBioSetup("")
        Truth.assertThat(
            result is BioSetupUseCase.BioFailedValidation.BioFailedEmpty
        ).isTrue()
    }

    @Test
    fun `given bioInfo is blank space when validationBioSetup then return BioFailedEmpty`() {
        val result = bioSetupUseCase.validationBioSetup(" ")
        Truth.assertThat(
            result is BioSetupUseCase.BioFailedValidation.BioFailedEmpty
        ).isTrue()
    }

    @Test
    fun `given bioInfo has less than 3 chars when validationBioSetup then return MinBioFailed`() {
        val result = bioSetupUseCase.validationBioSetup("te")
        Truth.assertThat(
            result is BioSetupUseCase.BioFailedValidation.MinBioFailed
        ).isTrue()
    }

    @Test
    fun `given bioInfo has more than 3 chars when validationBioSetup then return Success`() {
        val result = bioSetupUseCase.validationBioSetup(
            """
                test_test_test_test_test_test_test_test_test_test_test_test_test_test_test_test_
                test_test_test_test_test_test_test_test_test_test_test_test_test_test_test_test_
                test_test_test_test_test_test_test_test_test_test_test_test_test_test_test_test_
                test_test_test_test_test_test_test_test_test_test_test_test_test_test_test_test 
                """
        )
        Truth.assertThat(
            result is BioSetupUseCase.BioFailedValidation.Success
        ).isTrue()
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
        bioSetupUseCase = BioSetupUseCase(mockedContext, apiServices)
        val response = bioSetupUseCase("fake bio")
        println("network error test response = $response")
        Truth.assertThat(
            (response is BioSetupUseCase.BioSetupResult.ErrorType)
                    && (response.exception is NoNetworkException)
        ).isTrue()
        unmockkObject(NetworkUtils)
    }

    @Test
    fun `validate error response`() = runTest {
        val mockResponse = MockResponse()
        mockResponse.setBody(
            gson.toJson(
                BaseResponseModel<JsonObject>(
                    status = ApiConstants.RESPONSE_FAIL,
                    data = null,
                    statusCode = 201,
                    message = null
                )
            )
        )
        webServer.enqueue(mockResponse)
        val response = bioSetupUseCase("fake api")
        println("network error test response = $response")
        Truth.assertThat(
            (response is BioSetupUseCase.BioSetupResult.ErrorType)
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

        val response = bioSetupUseCase("fake bio")
        println("network error test response = $response")
        Truth.assertThat(response is BioSetupUseCase.BioSetupResult.StateSessionTimeout).isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        val mockResponse = MockResponse()
        mockResponse.setBody(
            gson.toJson(
                BaseResponseModel(
                    status = ApiConstants.RESPONSE_SUCCESS,
                    data = null,
                    statusCode = ApiConstants.RESPONSE_SUCCESS_CODE,
                    message = null
                )
            )
        )
        webServer.enqueue(mockResponse)
        val response = bioSetupUseCase("fake api")
        println("network error test response = $response")
        Truth.assertThat(response is BioSetupUseCase.BioSetupResult.Success).isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
        //Shutting down the server  after each test case.
        webServer.shutdown()
    }
}