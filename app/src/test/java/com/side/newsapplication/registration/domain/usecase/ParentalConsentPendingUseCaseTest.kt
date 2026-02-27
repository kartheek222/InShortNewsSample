package com.onexp.remag.registration.domain.usecase

import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SessionTimeoutResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.repository.network.ApiServices
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Created by ismail.akhtar on 09-05-2024.
 */
@ExtendWith(MainDispatcherExtension::class)
class ParentalConsentPendingUseCaseTest {

    private lateinit var apiServices: ApiServices
    private lateinit var parentalConsentPendingUseCase: ParentalConsentPendingUseCase

    @BeforeEach
    fun setUp(){
        apiServices = mockk<ApiServices>()
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "FakeDeviceId"
        parentalConsentPendingUseCase = ParentalConsentPendingUseCase(apiServices)
    }

    @AfterEach
    fun tearDown(){
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `validate onboarding status api with network error case`() = runTest {
        coEvery { apiServices.requestOnBoardingStatus() } throws NoNetworkException()
        val result = parentalConsentPendingUseCase()
        assertThat(result).isInstanceOf(ParentalConsentPendingUseCase.ParentalConsentPendingUseCaseState.ErrorState::class.java)
        assertThat(
            (result as ParentalConsentPendingUseCase.ParentalConsentPendingUseCaseState.ErrorState).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate onboarding status api with error case`() = runTest {
        val errorResponseModel  = ErrorResponseModel<Any>()
        coEvery { apiServices.requestOnBoardingStatus() } returns ErrorResponseModel()
        val result = parentalConsentPendingUseCase()
        assertThat(result).isInstanceOf(ParentalConsentPendingUseCase.ParentalConsentPendingUseCaseState.ErrorState::class.java)
        assertThat(
            (result as ParentalConsentPendingUseCase.ParentalConsentPendingUseCaseState.ErrorState).value?.equals(errorResponseModel)
        ).isTrue()
    }

    @Test
    fun `validate onboarding status api with session time out error case`() = runTest {
        val sessionTimeOutResponseModel = SessionTimeoutResponseModel<Any>()
        coEvery { apiServices.requestOnBoardingStatus() } returns SessionTimeoutResponseModel()
        val result = parentalConsentPendingUseCase()
        assertThat(result).isInstanceOf(ParentalConsentPendingUseCase.ParentalConsentPendingUseCaseState.SessionTimeOutErrorState::class.java)
        assertThat(
            (result as ParentalConsentPendingUseCase.ParentalConsentPendingUseCaseState.SessionTimeOutErrorState).value?.equals(sessionTimeOutResponseModel)
        ).isTrue()
    }

    @Test
    fun `validate onboarding status api with success case`() = runTest {
        val successResponseModel = SuccessResponseModel<Any>()
        coEvery { apiServices.requestOnBoardingStatus() } returns SuccessResponseModel()
        val result = parentalConsentPendingUseCase()
        assertThat(result).isInstanceOf(ParentalConsentPendingUseCase.ParentalConsentPendingUseCaseState.SuccessState::class.java)
        assertThat(
            (result as ParentalConsentPendingUseCase.ParentalConsentPendingUseCaseState.SuccessState).value?.equals(successResponseModel)
        ).isTrue()
    }
}