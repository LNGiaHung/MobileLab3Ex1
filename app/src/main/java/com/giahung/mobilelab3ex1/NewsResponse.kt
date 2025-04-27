package com.giahung.mobilelab3ex1

data class NewsResponse(
    val articles: List<Article>,
    val totalResults: Int
)

data class Article(
    val title: String,
    val content: String?,
    val urlToImage: String?,
    val publishedAt: String
)