package com.promise.promise.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// 로그인 요청 데이터 모델
data class LoginRequest(
    val name: String,
    val age: Int,
    val bottleId: String
)

// 서버 응답 모델
data class LoginResponse(
    val message: String
)

// Retrofit 인터페이스 정의
interface LoginService {
    @POST("/api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}