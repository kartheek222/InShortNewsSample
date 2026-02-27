package com.side.newsapplication.registration.domain.usecase

import com.google.common.truth.Truth
import com.side.newsapplication.data.ErrorResponseModel
import com.side.newsapplication.data.SessionTimeoutResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.EmailAvailabilityResponseModel
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EmailValidationUseCaseTest {
    private lateinit var apiService: ApiServices
    private lateinit var useCase: EmailValidationUseCase

    @BeforeEach
    fun setUp() {
        apiService = mockk<ApiServices>()
        useCase = EmailValidationUseCase(apiService)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }


    @Test
    fun `'validate email with error cases'`() {
        var result = useCase.invoke(null)
        Truth.assertThat(result)
            .isInstanceOf(EmailValidationUseCase.EmailCaseResult.ErrorType::class.java)

        result = useCase.invoke("")
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.ErrorType
                    && result.error is EmailValidationUseCase.EmailErrorType.EmailEmptyError
        ).isTrue()

        result = useCase.invoke("test")
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.ErrorType
                    && result.error is EmailValidationUseCase.EmailErrorType.EmailIncorrect
        ).isTrue()

        result = useCase.invoke("test.com")
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.ErrorType
                    && result.error is EmailValidationUseCase.EmailErrorType.EmailIncorrect
        ).isTrue()

        result = useCase.invoke("test@domain")
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.ErrorType
                    && result.error is EmailValidationUseCase.EmailErrorType.EmailIncorrect
        ).isTrue()//

        result = useCase.invoke("@domain.com")
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.ErrorType
                    && result.error is EmailValidationUseCase.EmailErrorType.EmailIncorrect
        ).isTrue()

        result = useCase.invoke("test.com")
        Truth.assertThat(
            result is EmailValidationUseCase.EmailCaseResult.ErrorType
                    && result.error is EmailValidationUseCase.EmailErrorType.EmailIncorrect
        ).isTrue()
    }

    @Test
    fun `'validate email with success cases'`() {
        val result = useCase.invoke("test@domain.com")
        Truth.assertThat(result)
            .isInstanceOf(EmailValidationUseCase.EmailCaseResult.Success::class.java)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiService.emailAvailability(any()) } throws NoNetworkException()
        val testResult = useCase.emailAvailability("")
        Truth.assertThat(
            testResult is EmailValidationUseCase.RequestEmailUseCaseResult.StateError && testResult.exception is NoNetworkException
        ).isTrue()
    }


    @Test
    fun `validate error response`() = runTest {
        coEvery { apiService.emailAvailability(any()) } returns ErrorResponseModel()
        val testResult = useCase.emailAvailability("test@domain.com")
        Truth.assertThat(testResult is EmailValidationUseCase.RequestEmailUseCaseResult.StateError && testResult.response?.status == ApiConstants.RESPONSE_FAIL)
            .isTrue()
    }

    @Test
    fun `validate error case with empty response in timeout case`() = runTest {
        val responseModel = SessionTimeoutResponseModel<EmailAvailabilityResponseModel?>()
        coEvery { apiService.emailAvailability(any()) } returns responseModel
        val testResult = useCase.emailAvailability("test@domain.com")
        Truth.assertThat(testResult is EmailValidationUseCase.RequestEmailUseCaseResult.StateSessionTimeout && testResult.response == responseModel)
            .isTrue()
    }

    @Test
    fun `validate error case with empty response in success case`() = runTest {
        val responseModel = SuccessResponseModel<EmailAvailabilityResponseModel?>()
        coEvery { apiService.emailAvailability(any()) } returns responseModel
        val testResult = useCase.emailAvailability("test@domain.com")
        Truth.assertThat(testResult is EmailValidationUseCase.RequestEmailUseCaseResult.StateError && testResult.response == responseModel)
            .isTrue()
    }

    @Test
    fun `validate response in success case`() = runTest {
        val responseModel = SuccessResponseModel<EmailAvailabilityResponseModel?>(
            data = EmailAvailabilityResponseModel(
                email = "test_domain.com",
                isAvailable = true
            )
        )
        coEvery { apiService.emailAvailability(any()) } returns responseModel
        val testResult = useCase.emailAvailability("test@domain.com")
        Truth.assertThat(testResult is EmailValidationUseCase.RequestEmailUseCaseResult.StateSuccess && testResult.response == responseModel)
            .isTrue()
    }

}