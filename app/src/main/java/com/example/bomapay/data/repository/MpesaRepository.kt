package com.example.bomapay.data.repository

import android.util.Base64
import com.example.bomapay.data.network.mpesa.MpesaService
import com.example.bomapay.data.network.mpesa.StkPushRequest
import com.example.bomapay.data.network.mpesa.StkPushResponse
import com.example.bomapay.data.network.mpesa.StkQueryRequest
import com.example.bomapay.data.network.mpesa.StkQueryResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MpesaRepository(
    private val consumerKey: String,
    private val consumerSecret: String,
    private val passKey: String,
    private val shortCode: String,
    private val callbackUrl: String
) {
    private val baseUrl = "https://sandbox.safaricom.co.ke/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val mpesaService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(MpesaService::class.java)

    private suspend fun getAccessToken(): String {
        val authString = "$consumerKey:$consumerSecret"
        val encodedAuth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
        val response = mpesaService.getAccessToken("Basic $encodedAuth")
        return response.accessToken
    }

    suspend fun initiateStkPush(phoneNumber: String, amount: Int, accountRef: String): Result<StkPushResponse> {
        return try {
            val token = getAccessToken()
            val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
            val password = Base64.encodeToString(
                "$shortCode$passKey$timestamp".toByteArray(),
                Base64.NO_WRAP
            )

            // Clean phone number: remove '+' or leading '0' and use '254'
            val cleanPhone = when {
                phoneNumber.startsWith("+") -> phoneNumber.substring(1)
                phoneNumber.startsWith("0") -> "254" + phoneNumber.substring(1)
                else -> phoneNumber
            }

            val request = StkPushRequest(
                businessShortCode = shortCode,
                password = password,
                timestamp = timestamp,
                amount = amount,
                partyA = cleanPhone,
                partyB = shortCode,
                phoneNumber = cleanPhone,
                callBackURL = callbackUrl,
                accountReference = accountRef,
                transactionDesc = "Rent Payment for $accountRef"
            )

            val response = mpesaService.sendStkPush("Bearer $token", request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun queryPaymentStatus(checkoutRequestID: String): Result<StkQueryResponse> {
        return try {
            val token = getAccessToken()
            val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
            val password = Base64.encodeToString(
                "$shortCode$passKey$timestamp".toByteArray(),
                Base64.NO_WRAP
            )

            val request = StkQueryRequest(shortCode, password, timestamp, checkoutRequestID)
            val response = mpesaService.queryStkPushStatus("Bearer $token", request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
