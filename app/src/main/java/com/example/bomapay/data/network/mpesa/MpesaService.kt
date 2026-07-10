package com.example.bomapay.data.network.mpesa

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface MpesaService {
    @GET("oauth/v1/generate?grant_type=client_credentials")
    suspend fun getAccessToken(
        @Header("Authorization") authHeader: String
    ): AccessTokenResponse

    @POST("mpesa/stkpush/v1/query")
    suspend fun queryStkPushStatus(
        @Header("Authorization") authHeader: String,
        @Body request: StkQueryRequest
    ): StkQueryResponse

    @POST("mpesa/stkpush/v1/processrequest")
    suspend fun sendStkPush(
        @Header("Authorization") authHeader: String,
        @Body request: StkPushRequest
    ): StkPushResponse
}
