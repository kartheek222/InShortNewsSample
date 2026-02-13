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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(val newsRepository: NewsRepository) : ViewModel() {

    private val _state: MutableStateFlow<ResponseState<NewsResponseModel>> =
        MutableStateFlow(ResponseState.Loading())
    val state = _state.asStateFlow()

    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        getNews()
    }

    fun getNews() {
        viewModelScope.launch(Dispatchers.IO) {
            newsRepository.getNewsDataSource("us").collectLatest {
                Timber.d("getNews : State: $it")
                if (it !is ResponseState.Loading) {
                    _isRefreshing.postValue(false)
                }
                _state.value = it
            }
        }
    }

    fun updateRefreshing(isRefreshing: Boolean) {
        _isRefreshing.postValue(isRefreshing)
    }
}