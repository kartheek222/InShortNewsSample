package com.side.newsapplication.home.presentation.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.runtime.mutableStateListOf
import com.google.common.truth.Truth
import com.onexp.remag.MainDispatcherExtension
import com.onexp.remag.base.data.BaseResponseModel
import com.side.newsapplication.data.ErrorResponseModel
import com.side.newsapplication.data.SuccessResponseModel
import com.onexp.remag.domain.NoNetworkException
import com.onexp.remag.home.data.GetTeamUpFilterModel
import com.onexp.remag.home.data.PlatformAndEndorsementMasterDataModel
import com.onexp.remag.home.domain.GetTeamUpFilterUseCase
import com.onexp.remag.home.domain.PlatformAndEndorsementMasterDataUseCase
import com.onexp.remag.home.domain.SetTeamUpFilterUseCase
import com.onexp.remag.registration.data.FavouriteGames
import com.onexp.remag.registration.data.InterestCategoriesResponseModel
import com.onexp.remag.registration.domain.usecase.FavouriteGameUseCase
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
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Created by Sonu.Sinha  on 5/9/2024
 */
@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class FriendsFragmentViewModelTest {

    lateinit var apiServices: ApiServices
    lateinit var mockedContext: Context
    lateinit var systemUnderTest: TeamUpFilterViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @BeforeEach
    fun setUp() {
        apiServices = mockk<ApiServices>()
        mockedContext = mockk<Context>(relaxed = true)
        systemUnderTest = TeamUpFilterViewModel(
            FavouriteGameUseCase(apiServices),
            SearchInterestUseCase(mockedContext, apiServices),
            GetTeamUpFilterUseCase(apiServices),
            PlatformAndEndorsementMasterDataUseCase(apiServices),
            SetTeamUpFilterUseCase(apiServices)
        )
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
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestFavouriteGames("test")
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSError).type as TeamUpFilterViewModel.TeamUpFilterUiStateType.GamesResponse).response == errorResponseModel
        ).isTrue()

    }

    @Test
    fun `validate network error for requestFavouriteGame`() = runTest() {
        coEvery { apiServices.requestFavouriteGames(any()) } throws NoNetworkException()
        // stateList is for collecting the Ui State of _gamerIdResponse in view-model.
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //api call
        systemUnderTest.requestFavouriteGames("test")
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError && (systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate network error for searchInterests`() = runTest() {
        coEvery { apiServices.searchInterests(any(), any()) } throws NoNetworkException()
        // stateList is for collecting the Ui State of _gamerIdResponse in view-model.
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //api call
        systemUnderTest.searchInterest("test")
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError && (systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate network error for requestTeamupFilter`() = runTest() {
        coEvery { apiServices.getTeamUpFilter() } throws NoNetworkException()
        // stateList is for collecting the Ui State of _gamerIdResponse in view-model.
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //api call
        systemUnderTest.requestTeamupFilter()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError && (systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate network error for requestPlatformAndEndorsementMasterData`() = runTest() {
        coEvery { apiServices.requestPlatformsAndEndorsementMasterData() } throws NoNetworkException()
        // stateList is for collecting the Ui State of _gamerIdResponse in view-model.
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //api call
        systemUnderTest.requestPlatformAndEndrosementMasterData()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError && (systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate network error for requestSetTeamupFilter`() = runTest() {
        coEvery { apiServices.setTeamUpFilter(any()) } throws NoNetworkException()
        // stateList is for collecting the Ui State of _gamerIdResponse in view-model.
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        //api call
        systemUnderTest.requestSetTeamupFilter()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError && (systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSError).exception is NoNetworkException
        ).isTrue()
    }

    @Test
    fun `validate requestPlatformAndEndorsementMasterData with error response`() = runTest {
        val errorResponseModel: BaseResponseModel<PlatformAndEndorsementMasterDataModel?> =
            ErrorResponseModel(
                PlatformAndEndorsementMasterDataModel(
                    endorsements = arrayListOf(),
                    platforms = arrayListOf(),
                )
            )
        coEvery { apiServices.requestPlatformsAndEndorsementMasterData() } returns errorResponseModel
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestPlatformAndEndrosementMasterData()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSError).type as TeamUpFilterViewModel.TeamUpFilterUiStateType.PlatformAndEndorsementMasterDataResponse).response == errorResponseModel
        ).isTrue()

    }

    @Test
    fun `validate searchInterests with error response`() = runTest {
        val errorResponseModel: BaseResponseModel<List<InterestCategoriesResponseModel>?> =
            ErrorResponseModel(
                data = arrayListOf()
            )
        coEvery { apiServices.searchInterests(any(), any()) } returns errorResponseModel
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.searchInterest("test")
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSError).type as TeamUpFilterViewModel.TeamUpFilterUiStateType.InterestResponse).response == errorResponseModel
        ).isTrue()

    }

    @Test
    fun `validate requestSetTeamupFilter with error response`() = runTest {
        val errorResponseModel: BaseResponseModel<Unit> =
            ErrorResponseModel()
        coEvery { apiServices.setTeamUpFilter(any()) } returns errorResponseModel
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestSetTeamupFilter()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSError
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSError).type as TeamUpFilterViewModel.TeamUpFilterUiStateType.SetTeamUpFilter).response == errorResponseModel
        ).isTrue()

    }

    @Test
    fun `validate requestPlatformAndEndorsementMasterData with success response`() = runTest {
        val successfulResponseModel: BaseResponseModel<PlatformAndEndorsementMasterDataModel?> =
            SuccessResponseModel(
                PlatformAndEndorsementMasterDataModel(
                    endorsements = arrayListOf(),
                    platforms = arrayListOf(),
                )
            )
        coEvery { apiServices.requestPlatformsAndEndorsementMasterData() } returns successfulResponseModel
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestPlatformAndEndrosementMasterData()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess).type as TeamUpFilterViewModel.TeamUpFilterUiStateType.PlatformAndEndorsementMasterDataResponse).response == successfulResponseModel
        ).isTrue()

    }

    @Test
    fun `validate searchInterests with success response`() = runTest {
        val successfulResponseModel: BaseResponseModel<List<InterestCategoriesResponseModel>?> =
            SuccessResponseModel(
                data = arrayListOf()
            )
        coEvery { apiServices.searchInterests(any(), any()) } returns successfulResponseModel
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.searchInterest("test")
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess).type as TeamUpFilterViewModel.TeamUpFilterUiStateType.InterestResponse).response == successfulResponseModel
        ).isTrue()

    }

    @Test
    fun `validate requestSetTeamupFilter with success response`() = runTest {
        val successfulResponseModel: BaseResponseModel<Unit> =
            SuccessResponseModel()
        coEvery { apiServices.setTeamUpFilter(any()) } returns successfulResponseModel
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestSetTeamupFilter()
        advanceUntilIdle()   // executing the scheduled call.
        //as stateList will collect for three times, i.e STATE_NONE,LOADING,ERROR
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(
            systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess
        ).isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess).type as TeamUpFilterViewModel.TeamUpFilterUiStateType.SetTeamUpFilter).response == successfulResponseModel
        ).isTrue()

    }

    @Test
    fun `validate requestTeamupFilter with success response`() = runTest {
        val successResponseModel: BaseResponseModel<GetTeamUpFilterModel?> =
            SuccessResponseModel(
                data =
                GetTeamUpFilterModel(
                    platforms = arrayListOf(),
                    games = arrayListOf(),
                    interests = arrayListOf(),
                    endorsements = arrayListOf()
                )
            )
        coEvery { apiServices.getTeamUpFilter() } returns successResponseModel
        val stateList =
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestTeamupFilter()
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess).type as TeamUpFilterViewModel.TeamUpFilterUiStateType.TeamupFilterResponse).response == successResponseModel
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
            mutableStateListOf<TeamUpFilterViewModel.TeamUpFilterUiState>()
        systemUnderTest.uiState.onEach {
            stateList.add(it)
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        systemUnderTest.requestFavouriteGames("test")
        advanceUntilIdle()
        Truth.assertThat(stateList).hasSize(3)
        Truth.assertThat(stateList[0] is TeamUpFilterViewModel.TeamUpFilterUiState.StateNone)
            .isTrue()
        Truth.assertThat(stateList[1] is TeamUpFilterViewModel.TeamUpFilterUiState.StateLoading)
            .isTrue()
        Truth.assertThat(stateList[2] is TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess)
            .isTrue()

        Truth.assertThat(systemUnderTest.uiState.value is TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess)
            .isTrue()
        Truth.assertThat(
            ((systemUnderTest.uiState.value as TeamUpFilterViewModel.TeamUpFilterUiState.StateSuccess).type as TeamUpFilterViewModel.TeamUpFilterUiStateType.GamesResponse).response == successResponseModel
        ).isTrue()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}