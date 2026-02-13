package com.sample.newsapplication.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.sample.newsapplication.ui.ErrorItem
import com.sample.newsapplication.ui.viewmodel.HomeViewModel
import com.sample.newsapplication.utilities.ResponseState
import com.sample.newsapplication.utilities.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navigator: NavHostController
) {
    Scaffold(
        modifier = Modifier,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("News")
                },
                actions = {
                    IconButton(onClick = {
                        navigator.navigate(Routes.SEARCH_SCREEN)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    }
                }
            )
        }) { innerPadding ->
        val newsState by viewModel.state.collectAsState()
        val isRefreshing by viewModel.isRefreshing.observeAsState(false)
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.updateRefreshing(true)
                    viewModel.getNews()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray)
            ) {
                when (newsState) {
                    is ResponseState.Loading -> {
//                        CircularLoader()
                        NewsArticleLoader()
                    }

                    is ResponseState.Success -> {
                        val newsList = (newsState as ResponseState.Success).data
                        NewsListScreen(responseModel = newsList)
                    }

                    is ResponseState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()), // Makes the area scrollable
                            contentAlignment = Alignment.Center
                        ) {
                            ErrorItem()
                        }
                    }

                    is ResponseState.None -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()), // Makes the area scrollable
                            contentAlignment = Alignment.Center
                        ) {

                        }
                    }

                }
            }

        }
    }
}