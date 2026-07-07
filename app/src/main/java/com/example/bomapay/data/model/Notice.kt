package com.example.bomapay.data.model

data class Notice(
    val noticeId: String = "",
    val title: String = "",
    val message: String = "",
    val targetHouse: String = "All", // Can be "All" or a specific house number like "A4"
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "noticeId" to noticeId,
            "title" to title,
            "message" to message,
            "targetHouse" to targetHouse,
            "createdAt" to createdAt
        )
    }
}