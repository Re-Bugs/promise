package com.promise.promise.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// 요청에 사용할 데이터 모델 정의
data class SignUpRequest(
    val name: String,
    val bottleId: String
)

// 서버 응답 모델 (필요시 수정)
data class SignUpResponse(
    val message: String
)

// Retrofit 인터페이스 정의
interface SignUpService {
    @POST("/api/sign_up")
    fun signUp(@Body request: SignUpRequest): Call<SignUpResponse>
}