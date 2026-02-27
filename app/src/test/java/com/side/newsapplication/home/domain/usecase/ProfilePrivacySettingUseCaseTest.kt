package com.onexp.remag.home.domain.usecase

import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SessionTimeoutResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.domain.ProfilePrivacySettingUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Created by ismail.akhtar on 15-05-2024.
 */
@ExtendWith(MainDispatcherExtension::class)
class ProfilePrivacySettingUseCaseTest {

    private lateinit var apiServices: ApiServices
    private lateinit var sut: ProfilePrivacySettingUseCase

    @BeforeEach
    fun setUp() {
        apiServices = mockk()
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceId"
        sut = ProfilePrivacySettingUseCase(apiServices)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `validate get profile privacy settings api with network error case`() = runTest {
        //mocking response
        coEvery { apiServices.getProfilePrivacy() } throws NoNetworkException()

        //hitting api
        val response = sut.getProfilePrivacy()

        //checking response type
        assertThat(response).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.StateSError::class.java)

        //checking exception
        assertThat(
            (response as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate get profile privacy settings api with error case`() = runTest {
        //mocking response
        coEvery { apiServices.getProfilePrivacy() } returns ErrorResponseModel()

        //hitting api
        val response = sut.getProfilePrivacy()

        //checking response type
        assertThat(response).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.StateSError::class.java)
        assertThat(
            (response as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.StateSError).responseType
        ).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.GetProfilePrivacySettingUseCaseResponseState::class.java)

        //checking response
        val errorResponseModel = ErrorResponseModel<Any>()
        assertThat(
            (response.responseType as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.GetProfilePrivacySettingUseCaseResponseState).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate get profile privacy settings api with session time out error case`() = runTest {
        //mocking response
        coEvery { apiServices.getProfilePrivacy() } returns SessionTimeoutResponseModel()

        //hitting api
        val response = sut.getProfilePrivacy()

        //checking response type
        assertThat(response).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.SessionTimeoutError::class.java)
        assertThat(
            (response as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.SessionTimeoutError).responseType
        ).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.GetProfilePrivacySettingUseCaseResponseState::class.java)

        //checking response
        val errorResponseModel = SessionTimeoutResponseModel<Any>()
        assertThat(
            (response.responseType as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.GetProfilePrivacySettingUseCaseResponseState).response?.equals(
                errorResponseModel
            )
        ).isTrue()
        assertThat(
            (response.responseType as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.GetProfilePrivacySettingUseCaseResponseState).response?.status.equals(
                ApiConstants.STATUS.IAT
            ) &&
                    (response.responseType as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.GetProfilePrivacySettingUseCaseResponseState).response?.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        ).isTrue()
    }

    @Test
    fun `validate get profile privacy settings api with success response case`() = runTest {
        //mocking response
        coEvery { apiServices.getProfilePrivacy() } returns SuccessResponseModel()

        //hitting api
        val response = sut.getProfilePrivacy()

        //checking response type
        assertThat(response).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.Success::class.java)
        assertThat(
            (response as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.Success).responseType
        ).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.GetProfilePrivacySettingUseCaseResponseState::class.java)

        //checking response
        val successResponseModel = SuccessResponseModel<Any>()
        assertThat(
            (response.responseType as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.GetProfilePrivacySettingUseCaseResponseState).response?.equals(
                successResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate post profile privacy settings api with network error case`() = runTest {
        //mocking response
        coEvery { apiServices.setProfilePrivacy(any()) } throws NoNetworkException()

        //hitting api
        val response = sut.postProfilePrivacy()

        //checking response type
        assertThat(response).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.StateSError::class.java)

        //checking exception
        assertThat(
            (response as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate post profile privacy settings api with error case`() = runTest {
        //mocking response
        coEvery { apiServices.setProfilePrivacy(any()) } returns ErrorResponseModel()

        //hitting api
        val response = sut.postProfilePrivacy()

        //checking response type
        assertThat(response).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.StateSError::class.java)
        assertThat(
            (response as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.StateSError).responseType
        ).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.PostProfilePrivacySettingUseCaseResponseState::class.java)

        //checking response
        val errorResponseModel = ErrorResponseModel<Any>()
        assertThat(
            (response.responseType as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.PostProfilePrivacySettingUseCaseResponseState).response?.equals(
                errorResponseModel
            )
        ).isTrue()
    }

    @Test
    fun `validate post profile privacy settings api with session time out error case`() = runTest {
        //mocking response
        coEvery { apiServices.setProfilePrivacy(any()) } returns SessionTimeoutResponseModel()

        //hitting api
        val response = sut.postProfilePrivacy()

        //checking response type
        assertThat(response).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.SessionTimeoutError::class.java)
        assertThat(
            (response as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.SessionTimeoutError).responseType
        ).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.PostProfilePrivacySettingUseCaseResponseState::class.java)

        //checking response
        val errorResponseModel = SessionTimeoutResponseModel<Any>()
        assertThat(
            (response.responseType as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.PostProfilePrivacySettingUseCaseResponseState).response?.equals(
                errorResponseModel
            )
        ).isTrue()
        assertThat(
            (response.responseType as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.PostProfilePrivacySettingUseCaseResponseState).response?.status.equals(
                ApiConstants.STATUS.IAT
            ) &&
                    (response.responseType as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.PostProfilePrivacySettingUseCaseResponseState).response?.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE
        ).isTrue()
    }

    @Test
    fun `validate post profile privacy settings api with success response case`() = runTest {
        //mocking response
        coEvery { apiServices.setProfilePrivacy(any()) } returns SuccessResponseModel()

        //hitting api
        val response = sut.postProfilePrivacy()

        //checking response type
        assertThat(response).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.Success::class.java)
        assertThat(
            (response as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResult.Success).responseType
        ).isInstanceOf(ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.PostProfilePrivacySettingUseCaseResponseState::class.java)

        //checking response
        val successResponseModel = SuccessResponseModel<Any>()
        assertThat(
            (response.responseType as ProfilePrivacySettingUseCase.ProfilePrivacySettingUseCaseResponseTypeState.PostProfilePrivacySettingUseCaseResponseState).response?.equals(
                successResponseModel
            )
        ).isTrue()
    }
}