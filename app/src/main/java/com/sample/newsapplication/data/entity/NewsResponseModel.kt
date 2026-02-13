package com.sample.newsapplication.data.entity


import com.google.gson.annotations.SerializedName

data class NewsResponseModel(
    @SerializedName("articles")
    val articles: List<ArticleModel>,
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String?,
    @SerializedName("totalResults")
    val totalResults: Int
)

data class SourceModel(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String
)

data class ArticleModel(
    @SerializedName("author")
    val author: String?,
    @SerializedName("content")
    val content: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("publishedAt")
    val publishedAt: String?,
    @SerializedName("source")
    val source: SourceModel?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("url")
    val url: String?,
    @SerializedName("urlToImage")
    val urlToImage: String?
)