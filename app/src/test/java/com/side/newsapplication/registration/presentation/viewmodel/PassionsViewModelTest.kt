package com.onexp.remag.registration.presentation.viewmodel

import android.content.Context
import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.domain.usecase.InterestCategoriesUseCase
import com.onexp.remag.registration.domain.usecase.SearchInterestUseCase
import com.onexp.remag.repository.network.ApiServices
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class PassionsViewModelTest {
    private lateinit var mockedContext: Context
    private lateinit var passionsViewModel: PassionsViewModel
    private lateinit var apiServices: ApiServices

    @BeforeEach
    fun setUp() {
        mockedContext = mockk<Context>(relaxed = true)
        apiServices = mockk<ApiServices>()
        passionsViewModel = PassionsViewModel(
            InterestCategoriesUseCase(mockedContext, apiServices),
            SearchInterestUseCase(mockedContext, apiServices)
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify request bio api with network error case`() = runTest {
        //Mocking the response
        coEvery { apiServices.getInterestCategories() } throws NoNetworkException()

        //Collecting the ui states
        val statesList = mutableListOf<PassionsViewModel.PassionUiState>()
        passionsViewModel.uiState.onEach(statesList::add)
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //Making the API request
        passionsViewModel.getInterestCategoriesList(true)
        //Wait until the response is executed.
        advanceUntilIdle()
        //Check the list size and values inside it.
        Truth.assertThat(statesList).hasSize(3)
        Truth.assertThat(statesList[0])
            .isInstanceOf(PassionsViewModel.PassionUiState.StateNone::class.java)
        Truth.assertThat(statesList[1])
            .isInstanceOf(PassionsViewModel.PassionUiState.StateLoading::class.java)
        Truth.assertThat(statesList[2])
            .isInstanceOf(PassionsViewModel.PassionUiState.StateError::class.java)
        //Checking the exception
        Truth.assertThat(
            (passionsViewModel.uiState.value is PassionsViewModel.PassionUiState.StateError)
                    && (passionsViewModel.uiState.value as PassionsViewModel.PassionUiState.StateError).exception is NoNetworkException
        ).isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}