package com.sample.newsapplication.data.datasource

import com.sample.newsapplication.data.entity.NewsResponseModel
import com.sample.newsapplication.utilities.AppConstants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsDataSource {

    suspend fun getNewsHeadline(
        country: String = "us",
    ): Response<NewsResponseModel>

    suspend fun searchNewsArticles(
        query: String
    ): Response<NewsResponseModel>
}