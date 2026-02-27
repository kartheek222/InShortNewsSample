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
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.GetTokenKeyForHeaderUseCase
import com.onexp.remag.domain.utils.NetworkUtils
import com.onexp.remag.registration.data.InterestCategoriesResponseModel
import com.onexp.remag.registration.data.InterestListResponseModel
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

class InterestCategoriesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantRule = InstantTaskExecutorRule()

    private lateinit var mockedContext: Context
    private lateinit var webServer: MockWebServer
    private lateinit var interestCategoriesUseCase: InterestCategoriesUseCase
    private lateinit var apiServices: ApiServices
    private val gson: Gson = GsonBuilder().create()

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceid"

        webServer = MockWebServer()
        apiServices = mockk<ApiServices>()
        interestCategoriesUseCase = InterestCategoriesUseCase(mockedContext, apiServices)
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
        interestCategoriesUseCase = InterestCategoriesUseCase(mockedContext, apiServices)

        val response = interestCategoriesUseCase()
        println("network error test response = $response")
        Truth.assertThat(response is InterestCategoriesUseCase.InterestCategoriesUseCaseResult.StateError)
            .isTrue()
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
        val response = interestCategoriesUseCase()
        println("network error test response = $response")
        Truth.assertThat(response is InterestCategoriesUseCase.InterestCategoriesUseCaseResult.StateError)
            .isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.getInterestCategories() } returns BaseResponseModel(
            data = null,
            status = ApiConstants.STATUS.IAT,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = interestCategoriesUseCase.invoke()
        Truth.assertThat(testResult is InterestCategoriesUseCase.InterestCategoriesUseCaseResult.StateSessionTimeout)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.getInterestCategories() } returns SuccessResponseModel(
            data = mutableListOf(
                InterestCategoriesResponseModel(
                    "test",
                    0, "test", arrayListOf(
                        InterestListResponseModel(
                            "", "", 0, null,
                            isCommon = true,
                            isAddedToMyProfile = true
                        )
                    )
                )
            )
        )
        val testResult = interestCategoriesUseCase.invoke()
        Truth.assertThat(testResult is InterestCategoriesUseCase.InterestCategoriesUseCaseResult.StateSuccess)
            .isTrue()
    }

    @Test
    fun `validate network error sendInterest`() = runTest {
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
        interestCategoriesUseCase = InterestCategoriesUseCase(mockedContext, apiServices)
        val data = mutableListOf(
            InterestListResponseModel(
                "", "", 0, null,
                isCommon = true,
                isAddedToMyProfile = true
            )
        )
        val response = interestCategoriesUseCase.sendInterest(data)
        println("network error test response = $response")
        Truth.assertThat(response is InterestCategoriesUseCase.InterestCategoriesUseCaseResult.StateError)
            .isTrue()
        unmockkObject(NetworkUtils)
    }

    @Test
    fun `validate error response sendInterest`() = runTest {
        val data = mutableListOf(
            InterestListResponseModel(
                "", "", 0, null,
                isCommon = true,
                isAddedToMyProfile = true
            )
        )
        val response = interestCategoriesUseCase.sendInterest(data)
        println("network error test response = $response")
        Truth.assertThat(response is InterestCategoriesUseCase.InterestCategoriesUseCaseResult.StateError)
            .isTrue()
    }

    @Test
    fun `validate success response sendInterest`() = runTest {
        val data = mutableListOf(
            InterestListResponseModel(
                "", "", 0, null,
                isCommon = true,
                isAddedToMyProfile = true
            )
        )
        coEvery { apiServices.sendInterests(any()) } returns SuccessResponseModel(
            data = JsonObject()
        )
        val response = interestCategoriesUseCase.sendInterest(data)
        Truth.assertThat(response is InterestCategoriesUseCase.InterestCategoriesUseCaseResult.StateSuccess)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
        //Shutting down the server  after each test case.
        webServer.shutdown()
    }
}