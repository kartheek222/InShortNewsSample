package com.onexp.remag.home.domain.usecase

import com.google.common.truth.Truth
import com.google.gson.JsonElement
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.domain.utils.NetworkUtils
import com.onexp.remag.home.domain.EditUserBioUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EditUserBioUseCaseTest {

    private lateinit var useCase: EditUserBioUseCase
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        useCase = EditUserBioUseCase(apiServices)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.editUserBio(any()) } throws NoNetworkException()
        val response = useCase("test")
        Truth.assertThat(
            (response is EditUserBioUseCase.BioSetupResult.ErrorType)
                    && (response.exception is NoNetworkException)
        ).isTrue()
        unmockkObject(NetworkUtils)
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery {
            apiServices.editUserBio(any())
        } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_FAIL,
            data = null,
            statusCode = 201,
            message = "Unable to process"
        )
        val response = useCase("")
        Truth.assertThat(
            response is EditUserBioUseCase.BioSetupResult.ErrorType && response.response?.status!! == ApiConstants.RESPONSE_FAIL
        ).isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.editUserBio(any()) } returns BaseResponseModel(
            status = ApiConstants.STATUS.IAT,
            data = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            message = null
        )
        val response = useCase("")
        Truth.assertThat(response is EditUserBioUseCase.BioSetupResult.StateSessionTimeout && response.response!!.statusCode == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()

    }

    @Test
    fun `validate success response`() = runTest {
        val mockResponse = mockk<JsonElement>()
        coEvery { apiServices.editUserBio(any()) } returns SuccessResponseModel(data = mockResponse)
        val response = useCase("")
        Truth.assertThat(response is EditUserBioUseCase.BioSetupResult.Success && response.response?.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @Test
    fun `validateBioSetup() when bioInfo is null, returns BioFailedEmpty`() {
        val result = useCase.validationBioSetup(null)
        Truth.assertThat(result is EditUserBioUseCase.BioFailedValidation.BioFailedEmpty)
            .isTrue()
    }

    @Test
    fun `validationBioSetup() when bioInfo is empty after trim, returns BioFailedEmpty`() {
        val result = useCase.validationBioSetup("   ")
        Truth.assertThat(result is EditUserBioUseCase.BioFailedValidation.BioFailedEmpty)
            .isTrue()
    }

    @Test
    fun `validationBioSetup() when bioInfo is less than MIN_BIO, returns MinBioFailed`() {
        val result = useCase.validationBioSetup("AB")
        Truth.assertThat(result is EditUserBioUseCase.BioFailedValidation.MinBioFailed).isTrue()
    }

    @Test
    fun `validationBioSetup() when bioInfo is valid, returns Success`() {
        val result = useCase.validationBioSetup("This is a valid bio.")
        Truth.assertThat(result is EditUserBioUseCase.BioFailedValidation.Success).isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

}