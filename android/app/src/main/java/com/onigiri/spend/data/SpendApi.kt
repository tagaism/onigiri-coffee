package com.onigiri.spend.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SpendApi {
    @POST("auth/register")
    suspend fun register(@Body body: AuthRequest): TokenResponse

    @POST("auth/login")
    suspend fun login(@Body body: AuthRequest): TokenResponse

    @GET("auth/me")
    suspend fun me(): User

    @GET("receipts")
    suspend fun listReceipts(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<ReceiptListItem>

    @GET("receipts/{id}")
    suspend fun getReceipt(@Path("id") id: String): Receipt

    @POST("receipts")
    suspend fun createReceipt(@Body body: ReceiptIn): Receipt

    @PATCH("receipts/{id}")
    suspend fun updateReceipt(@Path("id") id: String, @Body body: ReceiptIn): Receipt

    @DELETE("receipts/{id}")
    suspend fun deleteReceipt(@Path("id") id: String): Response<Unit>

    @GET("stats/summary")
    suspend fun summary(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Summary
}
