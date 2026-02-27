package com.side.newsapplication.home.presentation.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.BaseResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.data.ProfileResponse
import com.onexp.remag.home.domain.GetProfileDataUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class FriendingViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var mockedContext: Context
    private lateinit var viewModel: FriendingViewModel
    private lateinit var apiServices: ApiServices
    private lateinit var getProfileDataUseCase: GetProfileDataUseCase
    private lateinit var profileResponseModel: ProfileResponse

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        apiServices = mockk<ApiServices>()
        getProfileDataUseCase = GetProfileDataUseCase(apiServices)
        viewModel = FriendingViewModel(getProfileDataUseCase)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.requestProfileData(any()) } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_FAIL,
            data = null,
            statusCode = 400,
            message = "Unable to process"
        )
        val testResult = getProfileDataUseCase.invoke("")
        Truth.assertThat(
            (testResult is GetProfileDataUseCase.GetProfileDataUseCaseResult.StateSError && testResult.response?.status!! == ApiConstants.RESPONSE_FAIL)
        ).isTrue()
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.requestProfileData(any()) } throws NoNetworkException()
        val testResult = getProfileDataUseCase.invoke("")
        Truth.assertThat(
            (testResult is GetProfileDataUseCase.GetProfileDataUseCaseResult.StateSError && testResult.exception is NoNetworkException)
        ).isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        val response = mockk<ProfileResponse>()
        coEvery { apiServices.requestProfileData(any()) } returns SuccessResponseModel(response)
        val testResult = getProfileDataUseCase.invoke("")
        Truth.assertThat(testResult is GetProfileDataUseCase.GetProfileDataUseCaseResult.StateSuccess && testResult.response?.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @Test
    fun `validate session time out exception`() = runTest {
        coEvery { apiServices.requestProfileData(any()) } returns BaseResponseModel(
            data = null,
            status = ApiConstants.STATUS.IAT,
            message = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        )
        val testResult = getProfileDataUseCase.invoke("")
        Truth.assertThat(
            (testResult is GetProfileDataUseCase.GetProfileDataUseCaseResult.StateSessionTimeout && testResult.response?.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
        ).isTrue()
    }

}