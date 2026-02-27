package com.side.newsapplication.home.domain.usecase

import com.google.common.truth.Truth
import com.onexp.remag.base.data.BaseResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.domain.EditGamerTagsUseCase
import com.onexp.remag.home.data.GamerTagsRequest
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

class EditGamerTagsUseCaseTest {

    private lateinit var useCase: EditGamerTagsUseCase
    private lateinit var apiServices: ApiServices
    private val gamerTagRequest = mockk<GamerTagsRequest>()

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        useCase = EditGamerTagsUseCase(apiServices)
    }

    @Test
    fun `validate network error`() = runTest {
        coEvery { apiServices.editGamerTags(any()) } throws NoNetworkException()
        val response: EditGamerTagsUseCase.EditGamerTagsUseCaseResult =
            useCase.invoke(gamerTagRequest)
        Truth.assertThat(
            (response is EditGamerTagsUseCase.EditGamerTagsUseCaseResult.StateError)
                    && (response.exception is NoNetworkException)
        ).isTrue()
    }

    @Test
    fun `validate error response`() = runTest {
        coEvery { apiServices.editGamerTags(any()) } returns BaseResponseModel(
            status = ApiConstants.RESPONSE_FAIL,
            data = null,
            statusCode = 400,
            message = "Unable to process"
        )
        val response = useCase.invoke(gamerTagRequest)
        Truth.assertThat(
            response is EditGamerTagsUseCase.EditGamerTagsUseCaseResult.StateError && response.response?.status!! == ApiConstants.RESPONSE_FAIL
        ).isTrue()
    }

    @Test
    fun `validate session timeout error response`() = runTest {
        coEvery { apiServices.editGamerTags(any()) } returns BaseResponseModel(
            status = ApiConstants.STATUS.IAT,
            data = null,
            statusCode = ResponseInterceptor.UNAUTHENTICATED_STATUS_CODE,
            message = null
        )
        val response = useCase.invoke(gamerTagRequest)
        Truth.assertThat(response is EditGamerTagsUseCase.EditGamerTagsUseCaseResult.StateSessionTimeout)
            .isTrue()
    }

    @Test
    fun `validate success response`() = runTest {
        coEvery { apiServices.editGamerTags(any()) } returns SuccessResponseModel(Unit)
        val response = useCase.invoke(gamerTagRequest)
        Truth.assertThat(response is EditGamerTagsUseCase.EditGamerTagsUseCaseResult.StateSuccess && response.response?.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

}