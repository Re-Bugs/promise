package com.onlypromise.promise.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationService {

    // GET 요청: 알림 설정 조회
    @GET("/api/notification_value")
    fun getNotificationValue(@Query("bottleId") bottleId: String): Call<Map<String, String>>

    // PATCH 요청: 알림 설정 변경
    @PATCH("/api/notification_value")
    fun updateNotificationValue(@Query("bottleId") bottleId: String, @Query("value") value: String): Call<Map<String, String>>

    // GET 요청: 알림 시간 조회
    @GET("/api/alarm")
    fun getAlarmTimes(@Query("bottleId") bottleId: String): Call<Map<String, String>>

    // PATCH 요청: 알림 시간 변경
    @PATCH("/api/alarm")
    fun updateAlarmTimes(
        @Query("bottleId") bottleId: String,
        @Query("morningTime") morningTime: String,
        @Query("afternoonTime") afternoonTime: String,
        @Query("eveningTime") eveningTime: String
    ): Call<Map<String, String>>

    // 기존 /api/lookup/{bottleCode} 경로에 맞춰 수정
    @GET("/api/lookup/{bottleCode}")
    fun getNotifications(@Path("bottleCode") bottleCode: String): Call<Map<String, Any>>

    @GET("/api/daily_taken")
    fun getDailyTaken(
        @Query("bottleId") bottleId: String,
        @Query("date") date: String
    ): Call<Map<String, Any>>
}