package com.sample.newsapplication.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sample.newsapplication.data.entity.NewsResponseModel
import com.sample.newsapplication.ui.CircularLoader
import com.sample.newsapplication.ui.ErrorItem
import com.sample.newsapplication.ui.SearchTextField
import com.sample.newsapplication.ui.screens.home.NewsArticleLoader
import com.sample.newsapplication.ui.screens.home.NewsListScreen
import com.sample.newsapplication.ui.theme.NewsApplicationTheme
import com.sample.newsapplication.utilities.ResponseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: ResponseState<NewsResponseModel> = ResponseState.Loading(),
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Search")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onBackClick()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            /*  OutlinedTextField(
                  modifier = Modifier.fillMaxWidth()
                      .padding(horizontal = 10.dp),
                  shape = RoundedCornerShape(15.dp),
                  value = "Hello",
                  onValueChange = {},
                  placeholder = {
                      Text(text = "Search for topics")
                  },
                  leadingIcon = {
                      Icon(imageVector = Icons.Default.Search, contentDescription = null)
                  },
                  trailingIcon = {
                      IconButton(onClick = {

                      }) {
                          Icon(
                              imageVector = Icons.Default.Close, contentDescription = null
                          )
                      }
                  })*/
//            val query by viewModel.query.collectAsState()
            Spacer(modifier = Modifier.size(10.dp))
            SearchTextField(
                value = query,
                onValueChange = onQueryChange,
                onClear = onClear
            )
            Spacer(modifier = Modifier.size(10.dp))

            when (uiState) {
                is ResponseState.Loading -> {
//                    CircularLoader()
                    NewsArticleLoader()
                }

                is ResponseState.Success -> {
                    val newsList = uiState.data
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

@Preview
@Composable
fun SearchScreenPreview() {
    NewsApplicationTheme {
        SearchScreen(
            uiState = ResponseState.None(),
            query = "",
            onQueryChange = {},
            onClear = {},
            onBackClick = {})
    }
}