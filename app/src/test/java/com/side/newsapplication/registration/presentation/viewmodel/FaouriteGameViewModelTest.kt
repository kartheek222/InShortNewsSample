package com.onexp.remag.registration.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.runtime.mutableStateListOf
import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.BaseResponseModel
import com.onexp.remag.base.data.ErrorResponseModel
import com.onexp.remag.base.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.registration.data.FavouriteGames
import com.onexp.remag.registration.domain.usecase.FavouriteGameUseCase
import com.onexp.remag.registration.domain.usecase.SetFavouriteGameUseCase
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
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.manipulation.Ordering.Context

/**
 * Created by Sonu.Sinha  on 3/28/2024
 */
@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class FavouriteGameViewModelTest {

    lateinit var apiServices: ApiServices
    lateinit var mockedContext: Context
    lateinit var systemUnderTest: FavoriteGamesViewModel


    @get:Rule
    val rule = InstantTaskExecutorRule()

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        mockedContext = mockk<Context>(relaxed = true)
        systemUnderTest = FavoriteGamesViewModel(
            FavouriteGameUseCase(apiServices),
            SetFavouriteGameUseCase(apiServices)
        )

    }

    @Test
    fun `validate network error`() = runTest() {
        coEvery { apiServices.requestFavouriteGames(any()) } throws NoNetworkException()
        // stateList is for collecting the Ui State of _gamerIdResponse in view-model.
        val stateList =
            mutableStateListOf<FavoriteGamesViewModel.FavGameUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //api call
        systemUnderTest.requestFavouriteGames("test")
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is FavoriteGamesViewModel.FavGameUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is FavoriteGamesViewModel.FavGameUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is FavoriteGamesViewModel.FavGameUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is FavoriteGamesViewModel.FavGameUiState.StateSError && (systemUnderTest.uiState.value as FavoriteGamesViewModel.FavGameUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate requestFavouriteGame with error response`() = runTest {
        val errorResponseModel: BaseResponseModel<List<FavouriteGames>?> =
            ErrorResponseModel(
                data = listOf(
                    FavouriteGames(
                        "test",
                        "test", true, 111
                    )
                )
            )
        coEvery { apiServices.requestFavouriteGames(any()) } returns errorResponseModel
        val stateList =
            mutableStateListOf<FavoriteGamesViewModel.FavGameUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestFavouriteGames("test")
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is FavoriteGamesViewModel.FavGameUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is FavoriteGamesViewModel.FavGameUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is FavoriteGamesViewModel.FavGameUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is FavoriteGamesViewModel.FavGameUiState.StateSError
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as FavoriteGamesViewModel.FavGameUiState.StateSError).type as FavoriteGamesViewModel.FavouriteGameUIStateType.FavouriteGameResponse).response == errorResponseModel
        ).isTrue()

    }

    @Test
    fun `validate setfavouriteGames with error response`() = runTest {
        val errorResponseModel = ErrorResponseModel<Unit>()
        coEvery { apiServices.setFavouriteGames(any()) } returns errorResponseModel
        val stateList =
            mutableStateListOf<FavoriteGamesViewModel.FavGameUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestSetFavouriteGames()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is FavoriteGamesViewModel.FavGameUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is FavoriteGamesViewModel.FavGameUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is FavoriteGamesViewModel.FavGameUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is FavoriteGamesViewModel.FavGameUiState.StateSError
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as FavoriteGamesViewModel.FavGameUiState.StateSError).type as FavoriteGamesViewModel.FavouriteGameUIStateType.SetFavouriteGameResponse).response == errorResponseModel
        ).isTrue()

    }

    @Test
    fun `validate requestFavouriteGame with success response`() = runTest {
        val successResponseModel: BaseResponseModel<List<FavouriteGames>?> =
            SuccessResponseModel(
                data = listOf(
                    FavouriteGames(
                        "test",
                        "test", true, 11
                    )
                )
            )
        coEvery { apiServices.requestFavouriteGames(any()) } returns successResponseModel
        val stateList =
            mutableStateListOf<FavoriteGamesViewModel.FavGameUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestFavouriteGames("test")
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is FavoriteGamesViewModel.FavGameUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is FavoriteGamesViewModel.FavGameUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is FavoriteGamesViewModel.FavGameUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(systemUnderTest.uiState.value is FavoriteGamesViewModel.FavGameUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as FavoriteGamesViewModel.FavGameUiState.StateSuccess).type as FavoriteGamesViewModel.FavouriteGameUIStateType.FavouriteGameResponse).response == successResponseModel
        ).isTrue()
    }

    @Test
    fun `validate setFavouriteGames with success response`() = runTest {
        val successResponseModel = SuccessResponseModel(data = Unit)
        coEvery { apiServices.setFavouriteGames(any()) } returns successResponseModel
        val stateList =
            mutableStateListOf<FavoriteGamesViewModel.FavGameUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestSetFavouriteGames()
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is FavoriteGamesViewModel.FavGameUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is FavoriteGamesViewModel.FavGameUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is FavoriteGamesViewModel.FavGameUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(systemUnderTest.uiState.value is FavoriteGamesViewModel.FavGameUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as FavoriteGamesViewModel.FavGameUiState.StateSuccess).type as FavoriteGamesViewModel.FavouriteGameUIStateType.SetFavouriteGameResponse).response == successResponseModel
        ).isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}