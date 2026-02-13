package com.sample.newsapplication.data.api

import com.sample.newsapplication.data.entity.NewsResponseModel
import com.sample.newsapplication.utilities.AppConstants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET(AppConstants.HEAD_LINE_URL)
    suspend fun getNewsHeadline(
        @Query("country") country: String = "us",
        @Query("apiKey") apiKey: String = AppConstants.API_KEY
    ): Response<NewsResponseModel>

    @GET(AppConstants.SEARCH_NEWS_URL)
    suspend fun searchNewsArticles(
        @Query("sortBy") sortBy: String = "popularity",
        @Query("apiKey") apiKey: String = AppConstants.API_KEY,
        @Query("qInTitle") query: String
    ): Response<NewsResponseModel>
}