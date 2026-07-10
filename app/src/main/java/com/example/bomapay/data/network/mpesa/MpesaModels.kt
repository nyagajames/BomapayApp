package com.example.bomapay.data.network.mpesa

import com.google.gson.annotations.SerializedName

// 1. OAuth Access Token Response
data class AccessTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: String
)

// 2. STK Push Request
data class StkPushRequest(
    @SerializedName("BusinessShortCode") val businessShortCode: String,
    @SerializedName("Password") val password: String,
    @SerializedName("Timestamp") val timestamp: String,
    @SerializedName("TransactionType") val transactionType: String = "CustomerPayBillOnline",
    @SerializedName("Amount") val amount: Int,
    @SerializedName("PartyA") val partyA: String, // Phone number sending money
    @SerializedName("PartyB") val partyB: String, // Shortcode receiving money
    @SerializedName("PhoneNumber") val phoneNumber: String,
    @SerializedName("CallBackURL") val callBackURL: String,
    @SerializedName("AccountReference") val accountReference: String,
    @SerializedName("TransactionDesc") val transactionDesc: String
)

// 3. STK Push Response
data class StkPushResponse(
    @SerializedName("MerchantRequestID") val merchantRequestID: String,
    @SerializedName("CheckoutRequestID") val checkoutRequestID: String,
    @SerializedName("ResponseCode") val responseCode: String,
    @SerializedName("ResponseDescription") val responseDescription: String,
    @SerializedName("CustomerMessage") val customerMessage: String
)

// 4. STK Query Request
data class StkQueryRequest(
    @SerializedName("BusinessShortCode") val businessShortCode: String,
    @SerializedName("Password") val password: String,
    @SerializedName("Timestamp") val timestamp: String,
    @SerializedName("CheckoutRequestID") val checkoutRequestID: String
)

// 5. STK Query Response
data class StkQueryResponse(
    @SerializedName("ResponseCode") val responseCode: String, // "0" for request accepted
    @SerializedName("ResultCode") val resultCode: String?,   // "0" for success
    @SerializedName("ResultDesc") val resultDesc: String?
)
