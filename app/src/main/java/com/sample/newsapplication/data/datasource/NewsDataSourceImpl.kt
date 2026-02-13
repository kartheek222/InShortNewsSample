package com.sample.newsapplication.data.datasource

import com.sample.newsapplication.data.api.ApiService
import com.sample.newsapplication.data.entity.NewsResponseModel
import retrofit2.Response
import javax.inject.Inject


class NewsDataSourceImpl @Inject constructor(private val apiService: ApiService) : NewsDataSource {

    override suspend fun getNewsHeadline(country: String): Response<NewsResponseModel> {
        return apiService.getNewsHeadline(country = country)
    }

    override suspend fun searchNewsArticles(query: String): Response<NewsResponseModel> {
        return apiService.searchNewsArticles(query = query)
    }
}