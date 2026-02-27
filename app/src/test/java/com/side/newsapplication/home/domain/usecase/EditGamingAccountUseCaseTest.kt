package com.onexp.remag.home.domain.usecase

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.domain.EditGamingAccountUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import com.onexp.remag.repository.network.interceptors.ResponseInterceptor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EditGamingAccountUseCaseTest {

    private lateinit var useCase: EditGamingAccountUseCase
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        useCase = EditGamingAccountUseCase(apiServices)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.editGamingAccount(any()) } throws NoNetworkException()
        useCase = EditGamingAccountUseCase(apiServices)
        val response: EditGamingAccountUseCase.EditGamingAccountUseCaseResult = useCase.invoke(
            mutableListOf()
        )
        Truth.assertThat(
            (response is EditGamingAccountUseCase.EditGamingAccountUseCaseResult.StateSError)
                    && (response.exception is NoNetworkException)
        ).isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.editGamingAccount(any()) } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_FAIL,
            data = null,
            statusCode = 400,
            message = null
        )
        val response = useCase(mutableListOf())
        Truth.assertThat(
            response is EditGamingAccountUseCase.EditGamingAccountUseCaseResult.StateSError && response.response?.status!! == ApiConstants.RESPONSE_FAIL
        ).isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.editGamingAccount(any()) } returns BaseResponseModel(
            status = ApiConstants.STATUS.IAT,
            data = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            message = null
        )
        val response = useCase(mutableListOf())
        Truth.assertThat(response is EditGamingAccountUseCase.EditGamingAccountUseCaseResult.StateSessionTimeout && response.response?.statusCode!! == ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.editGamingAccount(any()) } returns SuccessResponseModel(Unit)
        val response = useCase(mutableListOf())
        Truth.assertThat(response is EditGamingAccountUseCase.EditGamingAccountUseCaseResult.StateSuccess && response.response?.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

}

