package com.side.newsapplication.home.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.onexp.remag.MainDispatcherExtension
import com.side.newsapplication.data.ERROR_MESSAGE_SESSION_EXPIRED
import com.side.newsapplication.data.ErrorResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.domain.utils.PKCEUtil
import com.onexp.remag.home.data.ReportCategoryItems
import com.onexp.remag.home.domain.RequestReportCategoriesUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import com.onexp.remag.repository.preferences.BasePreferencesManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class ReportViewModelTest{
    private lateinit var mockedContext: Context
    private lateinit var reportViewModel: ReportViewModel
    private lateinit var apiServices: ApiServices
    private lateinit var preferencesManager: BasePreferencesManager

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        apiServices = mockk<ApiServices>()
        mockkObject(PKCEUtil)
        every { PKCEUtil.generateCodeVerifier() } returns "fakeCodeVerifier"
        every { PKCEUtil.generateCodeChallenge(any()) } returns "fakeCodeVerifier"

        preferencesManager = mockk(relaxed = true)
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceId"

        mockkStatic(FirebaseMessaging::class)
        every { FirebaseMessaging.getInstance() } returns mockk(relaxed = true)

        reportViewModel = ReportViewModel(
            RequestReportCategoriesUseCase(apiServices)
        )
    }
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Settings.Secure::class)
        unmockkStatic(FirebaseMessaging::class)
        unmockkObject(PKCEUtil)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request report category list api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.getReportCategories(any()) } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<ReportViewModel.ReportUiState>()
        reportViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        reportViewModel.requestReportCategories(
                "fakeCategory"
            )
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(ReportViewModel.ReportUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(ReportViewModel.ReportUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(ReportViewModel.ReportUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(
            (reportViewModel.uiState.value is ReportViewModel.ReportUiState.StateSError)
                    && (reportViewModel.uiState.value as ReportViewModel.ReportUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request report category list api with error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<List<ReportCategoryItems>?>()
        coEvery { apiServices.getReportCategories(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<ReportViewModel.ReportUiState>()
        reportViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        reportViewModel.requestReportCategories(
            "fakeCategory"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(ReportViewModel.ReportUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(ReportViewModel.ReportUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(ReportViewModel.ReportUiState.StateSError::class.java)
        //Checking the exception
        Truth.assertThat(reportViewModel.uiState.value is ReportViewModel.ReportUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (reportViewModel.uiState.value as ReportViewModel.ReportUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request report category list api with session time out error case`() = runTest {
        val errorResponseModel = ErrorResponseModel<List<ReportCategoryItems>?>(
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            status = ApiConstants.STATUS.IAT,
            message = ERROR_MESSAGE_SESSION_EXPIRED
        )
        coEvery { apiServices.getReportCategories(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<ReportViewModel.ReportUiState>()
        reportViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        reportViewModel.requestReportCategories(
            "fakeCategory"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(ReportViewModel.ReportUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(ReportViewModel.ReportUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(ReportViewModel.ReportUiState.StateSessionTimeOut::class.java)
        //Checking the exception
        Truth.assertThat(reportViewModel.uiState.value is ReportViewModel.ReportUiState.StateSessionTimeOut)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            ((reportViewModel.uiState.value as ReportViewModel.ReportUiState.StateSessionTimeOut).type as ReportViewModel.ReportUiStateType.RequestReportCategoryState).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }
    @Test
    fun `verify request report category list api error case with empty data`() = runTest {
        val errorResponseModel = ErrorResponseModel<List<ReportCategoryItems>?>()
        coEvery { apiServices.getReportCategories(any()) } returns errorResponseModel

        //Collecting the ui states
        val statesList = mutableListOf<ReportViewModel.ReportUiState>()
        reportViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        reportViewModel.requestReportCategories(
            "fakeCategory"
        )
        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(ReportViewModel.ReportUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(ReportViewModel.ReportUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(ReportViewModel.ReportUiState.StateSError::class.java)
        //Checking the state in uiState
        Truth.assertThat(reportViewModel.uiState.value is ReportViewModel.ReportUiState.StateSError)
            .isTrue()
        //Checking the error type in the uiState
        Truth.assertThat(
            (reportViewModel.uiState.value as ReportViewModel.ReportUiState.StateSError).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }
    @Test
    fun `verify request otp api with success case`() = runTest {
        mockkStatic(FirebaseCrashlytics::class)
        //Mocking the fake response
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        coEvery { apiServices.getReportCategories(any()) } returns SuccessResponseModel(
            status = ApiConstants.RESPONSE_SUCCESS,
            data = mutableListOf(
                ReportCategoryItems(
                    "test",
                    "test",
                    0
                )
            ),
            statusCode = ApiConstants.RESPONSE_SUCCESS_CODE,
            )
        //it will contain states list
        val uiStatesList = mutableListOf<ReportViewModel.ReportUiState>()

        //Collecting the ui state
        reportViewModel.uiState
            .onEach { uiStatesList.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //Mocking the api
        reportViewModel.requestReportCategories(
            "fakeCategory"
        )

        //Wait until the response is executed.
        advanceUntilIdle()

        //Check the list size and values inside it.
        Truth.assertThat(uiStatesList).hasSize(3)
        Truth.assertThat(uiStatesList[0]).isInstanceOf(ReportViewModel.ReportUiState.StateNone::class.java)
        Truth.assertThat(uiStatesList[1]).isInstanceOf(ReportViewModel.ReportUiState.StateLoading::class.java)
        Truth.assertThat(uiStatesList[2]).isInstanceOf(ReportViewModel.ReportUiState.StateSuccess::class.java)

        Truth.assertThat((reportViewModel.uiState.value as ReportViewModel.ReportUiState.StateSuccess).type is ReportViewModel.ReportUiStateType.RequestReportCategoryState)
            .isTrue()
        unmockkStatic(FirebaseCrashlytics::class)
    }
}