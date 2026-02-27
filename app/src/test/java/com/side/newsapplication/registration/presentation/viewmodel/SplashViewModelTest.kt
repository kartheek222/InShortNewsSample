package com.side.newsapplication.registration.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.runtime.mutableStateListOf
import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.BaseResponseModel
import com.side.newsapplication.data.ERROR_MESSAGE_SESSION_EXPIRED
import com.side.newsapplication.data.ErrorResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.CheckConfigResponseModel
import com.onexp.remag.registration.data.FeatureFlags
import com.onexp.remag.registration.data.LatestVersion
import com.onexp.remag.registration.data.RegisterAuthTokenResponseModel
import com.onexp.remag.registration.domain.usecase.RequestOnBoardingStatusUseCase
import com.onexp.remag.registration.domain.usecase.ValidateConfigUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Created by Sonu.Sinha on 2/6/2024
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
class SplashViewModelTest {

    lateinit var systemUnderTest: SplashViewModel
    lateinit var apiServices: ApiServices

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        systemUnderTest = SplashViewModel(
            ValidateConfigUseCase(apiServices),
            RequestOnBoardingStatusUseCase(apiServices)
        )
    }


    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.validateAppVersion(any()) } throws NoNetworkException()
        val stateList = mutableStateListOf<SplashViewModel.SplashUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestValidateAppConfig()
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is SplashViewModel.SplashUiState.StateNone).isTrue()
        Truth.assertThat(stateList[1] is SplashViewModel.SplashUiState.StateLoading).isTrue()
        Truth.assertThat(stateList[2] is SplashViewModel.SplashUiState.StateSError).isTrue()
        Truth.assertThat(systemUnderTest.uiState.value is SplashViewModel.SplashUiState.StateSError)
            .isTrue()
        Truth.assertThat((systemUnderTest.uiState.value as SplashViewModel.SplashUiState.StateSError).exception is NoNetworkException)
    }

    @Test
    fun `validate appconfig for error response`() = runTest {
        val errorResponseModel = ErrorResponseModel<CheckConfigResponseModel>()
        coEvery { apiServices.validateAppVersion(any()) } returns errorResponseModel
        val stateList = mutableStateListOf<SplashViewModel.SplashUiState>()
        systemUnderTest.uiState.onEach(stateList::add).launchIn(
            CoroutineScope(
                UnconfinedTestDispatcher(testScheduler)
            )
        )
        systemUnderTest.requestValidateAppConfig()
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is SplashViewModel.SplashUiState.StateNone).isTrue()
        Truth.assertThat(stateList[1] is SplashViewModel.SplashUiState.StateLoading).isTrue()
        Truth.assertThat(stateList[2] is SplashViewModel.SplashUiState.StateSError).isTrue()
        Truth.assertThat(systemUnderTest.uiState.value is SplashViewModel.SplashUiState.StateSError)
            .isTrue()
        Truth.assertThat(((systemUnderTest.uiState.value as SplashViewModel.SplashUiState.StateSError).type as SplashViewModel.SplashUiStateType.StateValidateConfig).response == errorResponseModel)
            .isTrue()
    }

    @Test
    fun `validate session time out response`() = runTest {
        val errorResponseModel =
            BaseResponseModel<CheckConfigResponseModel>(
                statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
                data = null,
                status = ApiConstants.STATUS.IAT,
                message = ERROR_MESSAGE_SESSION_EXPIRED
            )
        coEvery { apiServices.validateAppVersion(any()) } returns errorResponseModel
        val stateList = mutableStateListOf<SplashViewModel.SplashUiState>()
        systemUnderTest.uiState.onEach(stateList::add).launchIn(
            CoroutineScope(
                UnconfinedTestDispatcher(testScheduler)
            )
        )
        systemUnderTest.requestValidateAppConfig()
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is SplashViewModel.SplashUiState.StateNone).isTrue()
        Truth.assertThat(stateList[1] is SplashViewModel.SplashUiState.StateLoading).isTrue()
        Truth.assertThat(stateList[2] is SplashViewModel.SplashUiState.SessionTimeOutError).isTrue()
        Truth.assertThat(systemUnderTest.uiState.value is SplashViewModel.SplashUiState.SessionTimeOutError)
            .isTrue()
        Truth.assertThat(((systemUnderTest.uiState.value as SplashViewModel.SplashUiState.SessionTimeOutError).type as SplashViewModel.SplashUiStateType.StateValidateConfig).response == errorResponseModel)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        val checkConfigResponseModel = CheckConfigResponseModel(
            FeatureFlags(
                avatarStore = false, false, false, false,
                teamUp = false,
                trading = false,
                watchParty = false
            ),
            LatestVersion(forceUpgrade = false, optionalUpgrade = false, 2, "3"), ""
        )
        val successResponse = SuccessResponseModel(data = checkConfigResponseModel)
        coEvery { apiServices.validateAppVersion(any()) } returns successResponse
        val stateList = mutableStateListOf<SplashViewModel.SplashUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestValidateAppConfig()
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is SplashViewModel.SplashUiState.StateNone).isTrue()
        Truth.assertThat(stateList[1] is SplashViewModel.SplashUiState.StateLoading).isTrue()
        Truth.assertThat(stateList[2] is SplashViewModel.SplashUiState.StateSuccess).isTrue()
        Truth.assertThat(systemUnderTest.uiState.value is SplashViewModel.SplashUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(stateList.last() is SplashViewModel.SplashUiState.StateSuccess).isTrue()
        Truth.assertThat(systemUnderTest.uiState.value is SplashViewModel.SplashUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(((systemUnderTest.uiState.value as SplashViewModel.SplashUiState.StateSuccess).type as SplashViewModel.SplashUiStateType.StateValidateConfig).response == successResponse)
            .isTrue()
    }

    @Test
    fun `validate appconfigAsync for success response`() = runTest {
        val configSuccessResponseModel = SuccessResponseModel(
            CheckConfigResponseModel(
                FeatureFlags(
                    avatarStore = false, false, false, false,
                    teamUp = false,
                    trading = false,
                    watchParty = false
                ),
                LatestVersion(forceUpgrade = false, optionalUpgrade = false, 2, "3"), ""
            )
        )
        val onBoardingSuccessResponseModel: BaseResponseModel<RegisterAuthTokenResponseModel?> =
            SuccessResponseModel(
                RegisterAuthTokenResponseModel(
                    authCode = "",
                    gamerId = "",
                    profilePic = "",
                    userId = "",
                    onboardingStatus = "",
                    url = "", guardianEmailId = ""
                )
            )
        coEvery { apiServices.validateAppVersion(any()) } returns configSuccessResponseModel
        coEvery { apiServices.requestOnBoardingStatus() } returns onBoardingSuccessResponseModel
        val stateList = mutableStateListOf<SplashViewModel.SplashUiState>()
        systemUnderTest.uiState.onEach(stateList::add).launchIn(
            CoroutineScope(
                UnconfinedTestDispatcher(testScheduler)
            )
        )
        systemUnderTest.requestValidateAppConfigAsync()
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is SplashViewModel.SplashUiState.StateNone).isTrue()
        Truth.assertThat(stateList[1] is SplashViewModel.SplashUiState.StateLoading).isTrue()
        Truth.assertThat(stateList[2] is SplashViewModel.SplashUiState.StateSuccess).isTrue()
        Truth.assertThat(systemUnderTest.uiState.value is SplashViewModel.SplashUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as SplashViewModel.SplashUiState.StateSuccess).type as SplashViewModel.SplashUiStateType.StateConfigAndOnBoardingStatus).confingResponse == configSuccessResponseModel
                    && ((systemUnderTest.uiState.value as SplashViewModel.SplashUiState.StateSuccess).type as SplashViewModel.SplashUiStateType.StateConfigAndOnBoardingStatus).onBoardingResponse == onBoardingSuccessResponseModel
        )
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}