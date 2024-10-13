package com.onlypromise.promise.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://onlypromise.com"

    // Retrofit 인스턴스 생성
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // loginService 추가
    val loginService: LoginService = createService(LoginService::class.java)

    // 모든 서비스 인터페이스 생성 메서드
    fun <T> createService(service: Class<T>): T {
        return retrofit.create(service)
    }

    val reportService: ReportService = createService(ReportService::class.java)
}