package com.sample.newsapplication.ui.repository

import com.sample.newsapplication.data.datasource.NewsDataSource
import com.sample.newsapplication.data.entity.NewsResponseModel
import com.sample.newsapplication.utilities.ResponseState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NewsRepository @Inject constructor(private val dataSource: NewsDataSource) {

    fun getNewsDataSource(country: String): Flow<ResponseState<NewsResponseModel>> {
        return flow {
            emit(ResponseState.Loading())

            val response = dataSource.getNewsHeadline(country = country)

            if (response.isSuccessful && response.body() != null) {
                emit(ResponseState.Success(response.body()!!))
            } else {
                emit(ResponseState.Error(error = response.body()!!))
            }
        }.catch { exception ->
            emit(ResponseState.Error(exception = exception))
        }
    }

    fun searchNewsDataSource(query: String): Flow<ResponseState<NewsResponseModel>> {
        return flow {
            emit(ResponseState.Loading())

            val response = dataSource.searchNewsArticles(query = query)

            if (response.isSuccessful && response.body() != null) {
                emit(ResponseState.Success(response.body()!!))
            } else {
                emit(ResponseState.Error(error = response.body()!!))
            }
        }.catch { exception ->
            emit(ResponseState.Error(exception = exception))
        }
    }

}