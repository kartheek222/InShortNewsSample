package com.onexp.remag.home.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SessionTimeoutResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.data.ProfilePrivacySettingModel
import com.onexp.remag.home.domain.ClearLoginPreferencesUseCase
import com.onexp.remag.home.domain.ProfilePrivacySettingUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import com.onexp.remag.repository.preferences.BasePreferencesManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)

/**
 * Created by ismail.akhtar on 16-05-2024.
 */
@ExtendWith(MainDispatcherExtension::class)
class ProfilePrivacySettingViewModelTest {

    private lateinit var profilePrivacySettingUseCase: ProfilePrivacySettingUseCase
    private lateinit var apiServices: ApiServices
    private lateinit var sut: ProfilePrivacySettingViewModel

    @BeforeEach
    fun setUp() {
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceId"
        apiServices = mockk()
        profilePrivacySettingUseCase = ProfilePrivacySettingUseCase(apiServices)
        val preferencesManager = mockk<BasePreferencesManager>()
        val clearLoginPreferencesUseCase = ClearLoginPreferencesUseCase(
            context = mockk<Context>(),
            generalPreferences = preferencesManager,
            encryptedPreferences = preferencesManager
        )
        sut = ProfilePrivacySettingViewModel(
            profilePrivacySettingUseCase,
            clearLoginPreferencesUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `validate get profile privacy settings api with network error case`() = runTest {
        //mocking response
        coEvery { apiServices.getProfilePrivacy() } throws NoNetworkException()

        //collecting ui states
        val uiStates = mutableListOf<ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.getProfilePrivacyVisibilityList()

        //wait until all operations completed
        advanceUntilIdle()

        //checking ui states
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError::class.java)

        //checking network exception
        assertThat(
            (sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate get profile privacy settings api with error case`() = runTest {
        //mocking response
        coEvery { apiServices.getProfilePrivacy() } returns ErrorResponseModel()

        //collecting ui states
        val uiStates = mutableListOf<ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.getProfilePrivacyVisibilityList()

        //wait until all operations completed
        advanceUntilIdle()

        //checking ui states
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError::class.java)
        assertThat(
            (sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError).responseType
        ).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.GetProfilePrivacySettingUIResponseState::class.java)

        //Checking error
        val errorResponseModel = ErrorResponseModel<Any>()
        assertThat(
            ((sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError).responseType as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.GetProfilePrivacySettingUIResponseState).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate get profile privacy settings api with session time out error case`() = runTest {
        //mocking response
        coEvery { apiServices.getProfilePrivacy() } returns SessionTimeoutResponseModel()

        //collecting ui states
        val uiStates = mutableListOf<ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.getProfilePrivacyVisibilityList()

        //wait until all operations completed
        advanceUntilIdle()

        //checking ui states
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.SessionTimeoutError::class.java)
        assertThat(
            (sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.SessionTimeoutError).responseType
        ).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.GetProfilePrivacySettingUIResponseState::class.java)

        //Checking error
        val errorResponseModel = SessionTimeoutResponseModel<Any>()
        assertThat(
            ((sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.SessionTimeoutError).responseType as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.GetProfilePrivacySettingUIResponseState).response?.equals(
                errorResponseModel
            )
        ).isTrue()

        assertThat(
            ((sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.SessionTimeoutError).responseType as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.GetProfilePrivacySettingUIResponseState).response?.status.equals(
                ApiConstants.STATUS.IAT
            )
                    && ((sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.SessionTimeoutError).responseType as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.GetProfilePrivacySettingUIResponseState).response?.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        ).isTrue()
    }

    @Test
    fun `validate get profile privacy settings api with success response but null data which fall in error case`() = runTest {
        //mocking response
        coEvery { apiServices.getProfilePrivacy() } returns SuccessResponseModel(data = null)

        //collecting ui states
        val uiStates = mutableListOf<ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.getProfilePrivacyVisibilityList()

        //wait until all operations completed
        advanceUntilIdle()

        //checking ui states
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError::class.java)
        assertThat(
            (sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError).responseType
        ).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.GetProfilePrivacySettingUIResponseState::class.java)
    }

    @Test
    fun `validate get profile privacy settings api with success response`() = runTest {
        //mocking response
        coEvery { apiServices.getProfilePrivacy() } returns SuccessResponseModel(data = ProfilePrivacySettingModel())

        //collecting ui states
        val uiStates = mutableListOf<ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.getProfilePrivacyVisibilityList()

        //wait until all operations completed
        advanceUntilIdle()

        //checking ui states
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Success::class.java)
        assertThat(
            (sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Success).responseType
        ).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.GetProfilePrivacySettingUIResponseState::class.java)

        //checking success response
        val successResponseModel = SuccessResponseModel(data = ProfilePrivacySettingModel())
        assertThat(
            ((sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Success).responseType as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.GetProfilePrivacySettingUIResponseState).response?.equals(
                successResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate post profile privacy settings api with network error case`() = runTest {
        //mocking response
        coEvery { apiServices.setProfilePrivacy(any()) } throws NoNetworkException()

        //collecting ui states
        val uiStates = mutableListOf<ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.postProfilePrivacyVisibilityList()

        //wait until all operations completed
        advanceUntilIdle()

        //checking ui states
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError::class.java)

        //checking network exception
        assertThat(
            (sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate post profile privacy settings api with error case`() = runTest {
        //mocking response
        coEvery { apiServices.setProfilePrivacy(any()) } returns ErrorResponseModel()

        //collecting ui states
        val uiStates = mutableListOf<ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.postProfilePrivacyVisibilityList()

        //wait until all operations completed
        advanceUntilIdle()

        //checking ui states
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError::class.java)
        assertThat(
            (sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError).responseType
        ).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.PostProfilePrivacySettingUIResponseState::class.java)

        //Checking error
        val errorResponseModel = ErrorResponseModel<Any>()
        assertThat(
            ((sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.StateSError).responseType as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.PostProfilePrivacySettingUIResponseState).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate post profile privacy settings api with session time out error case`() = runTest {
        //mocking response
        coEvery { apiServices.setProfilePrivacy(any()) } returns SessionTimeoutResponseModel()

        //collecting ui states
        val uiStates = mutableListOf<ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.postProfilePrivacyVisibilityList()

        //wait until all operations completed
        advanceUntilIdle()

        //checking ui states
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.SessionTimeoutError::class.java)
        assertThat(
            (sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.SessionTimeoutError).responseType
        ).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.PostProfilePrivacySettingUIResponseState::class.java)

        //Checking error
        val errorResponseModel = SessionTimeoutResponseModel<Any>()
        assertThat(
            ((sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.SessionTimeoutError).responseType as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.PostProfilePrivacySettingUIResponseState).response?.equals(
                errorResponseModel
            )
        ).isTrue()

        assertThat(
            ((sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.SessionTimeoutError).responseType as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.PostProfilePrivacySettingUIResponseState).response?.status.equals(
                ApiConstants.STATUS.IAT
            )
                    && ((sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.SessionTimeoutError).responseType as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.PostProfilePrivacySettingUIResponseState).response?.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        ).isTrue()
    }

    @Test
    fun `validate post profile privacy settings api with success response`() = runTest {
        //mocking response
        coEvery { apiServices.setProfilePrivacy(any()) } returns SuccessResponseModel()

        //collecting ui states
        val uiStates = mutableListOf<ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState>()
        sut.uiState.onEach {
            uiStates.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        //hitting api
        sut.postProfilePrivacyVisibilityList()

        //wait until all operations completed
        advanceUntilIdle()

        //checking ui states
        assertThat(uiStates).hasSize(3)
        assertThat(uiStates[0]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.None::class.java)
        assertThat(uiStates[1]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Loading::class.java)
        assertThat(uiStates[2]).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Success::class.java)
        assertThat(
            (sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Success).responseType
        ).isInstanceOf(ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.PostProfilePrivacySettingUIResponseState::class.java)

        //checking success response
        val successResponseModel = SuccessResponseModel<Any>()
        assertThat(
            ((sut.uiState.value as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiState.Success).responseType as ProfilePrivacySettingViewModel.ProfilePrivacySettingUiResponseTypeState.PostProfilePrivacySettingUIResponseState).response?.equals(
                successResponseModel
            )
        ).isTrue()
    }
}