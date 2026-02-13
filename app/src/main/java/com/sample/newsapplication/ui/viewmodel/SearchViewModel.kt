package com.sample.newsapplication.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sample.newsapplication.data.entity.NewsResponseModel
import com.sample.newsapplication.ui.repository.NewsRepository
import com.sample.newsapplication.utilities.ResponseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(val newsRepository: NewsRepository) : ViewModel() {

    private val _state: MutableStateFlow<ResponseState<NewsResponseModel>> =
        MutableStateFlow(ResponseState.None())
    val state = _state.asStateFlow()

    private val _query: MutableStateFlow<String> = MutableStateFlow("")
    val query = _query.asStateFlow()
    var searchJob: Job? = null

    init {
        viewModelScope.launch {
            query.map { it.trim() }
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { query ->
                    Timber.d("Query: $query")
                    if (query.isEmpty()) {
                        searchJob?.cancel()
                        searchJob = null
                        _state.value = ResponseState.None()
                    } else {
                        searchNews(query = query)
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun clearQuery() {
        _query.value = ""
    }


    fun searchNews(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            newsRepository.searchNewsDataSource(query = query).collectLatest {
                _state.value = it
            }
        }
    }
}