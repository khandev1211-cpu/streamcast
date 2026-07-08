package com.streamcast.core.network

import retrofit2.http.GET
import retrofit2.http.Query

interface IptvApiClient {
    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<XtreamCategoryResponse>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): List<XtreamStreamResponse>
}

data class XtreamCategoryResponse(
    val category_id: String,
    val category_name: String
)

data class XtreamStreamResponse(
    val stream_id: String,
    val name: String,
    val stream_icon: String?,
    val category_id: String,
    val stream_type: String
)
