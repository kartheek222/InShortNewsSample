package com.sample.newsapplication.ui.screens.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sample.newsapplication.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreenRoute(
    navigator: NavHostController
) {
    val viewModel: SearchViewModel = hiltViewModel<SearchViewModel>()
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val query by viewModel.query.collectAsState()

    SearchScreen(
        uiState = uiState,
        query = query,
        onQueryChange = viewModel::onQueryChange,
        onClear = viewModel::clearQuery,
        onBackClick = {
            navigator.popBackStack()
        }
    )
}