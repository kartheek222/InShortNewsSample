package com.side.newsapplication.home.presentation.viewmodel

import com.google.common.truth.Truth
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.data.GetFriendRequestsResponseModel
import com.onexp.remag.home.domain.BlockUserUseCase
import com.onexp.remag.home.domain.GetFriendRequestsUseCase
import com.onexp.remag.home.domain.UpdateFriendRequestUseCase
import com.onexp.remag.parentalConsent.domain.ParentalConsentStatusUseCase
import com.onexp.remag.repository.network.ApiConstants
import com.onexp.remag.repository.network.ApiServices
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FriendingRequestsViewModelTest {

    private lateinit var viewModel: FriendingRequestsViewModel
    private lateinit var apiServices: ApiServices
    private lateinit var friendingRequestsUseCase: GetFriendRequestsUseCase
    private lateinit var updateFriendRequestUseCase: UpdateFriendRequestUseCase
    private lateinit var blockUserUseCase: BlockUserUseCase
    private lateinit var parentalConsentStatusUsecase: ParentalConsentStatusUseCase

    @BeforeEach
    fun setup() {
        apiServices = mockk<ApiServices>()
        friendingRequestsUseCase = GetFriendRequestsUseCase(apiServices)
        updateFriendRequestUseCase = UpdateFriendRequestUseCase(apiServices)
        blockUserUseCase = BlockUserUseCase(apiServices)
        parentalConsentStatusUsecase = ParentalConsentStatusUseCase(apiServices)
        viewModel = FriendingRequestsViewModel(friendingRequestsUseCase, updateFriendRequestUseCase,blockUserUseCase,parentalConsentStatusUsecase)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

        @Test
        fun `getFriendRequests should emit StateSuccess when successful`() = runTest {
        val response = mockk<GetFriendRequestsResponseModel>()
        coEvery { apiServices.getFriendingRequest(any()) } returns SuccessResponseModel(response)
        val testResult = friendingRequestsUseCase.invoke("")
        Truth.assertThat(testResult is GetFriendRequestsUseCase.GetFriendRequestsUseCaseResult.StateSuccess && testResult.response.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()

    }

    @Test
    fun `getFriendRequests should emit StateError when an exception occurs`() = runTest {
        coEvery { apiServices.getFriendingRequest(any()) } throws NoNetworkException()
            val testResult: GetFriendRequestsUseCase.GetFriendRequestsUseCaseResult =
            friendingRequestsUseCase.invoke("")
        Truth.assertThat(testResult is GetFriendRequestsUseCase.GetFriendRequestsUseCaseResult.StateError && testResult.exception is NoNetworkException)
            .isTrue()
            }

    @Test
    fun `updateFriendRequest should emit StateSuccess when successful`() = runTest {
        coEvery { apiServices.updateFriendRequest(any()) } returns SuccessResponseModel(Unit)
            val testResult = updateFriendRequestUseCase.invoke("", true)
            Truth.assertThat(testResult is UpdateFriendRequestUseCase.UpdateFriendRequestCaseResult.StateSuccess && testResult.response.statusCode == ApiConstants.RESPONSE_SUCCESS_CODE)
            .isTrue()
        }

    @Test
    fun `updateFriendRequest should emit StateError when an exception occurs`() = runTest {
        coEvery { apiServices.updateFriendRequest(any()) } throws NoNetworkException()
        val testResult: UpdateFriendRequestUseCase.UpdateFriendRequestCaseResult =
            updateFriendRequestUseCase.invoke("", true)
        Truth.assertThat(testResult is UpdateFriendRequestUseCase.UpdateFriendRequestCaseResult.StateError && testResult.exception is NoNetworkException)
            .isTrue()
    }

    @Test
    fun `getFormattedDate should return the correct number of days`() {

        val daysSinceLastRequest = 5
        // Arrange
        val currentTime = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * daysSinceLastRequest)
        val epochTime = currentTime / 1000.0

        // Act
        val formattedDate =
            viewModel.getFormattedDate(epochTime) as FriendingRequestsViewModel.FormattedTime.Days

        // Assert
        assertEquals(daysSinceLastRequest, formattedDate.difference)
    }
}