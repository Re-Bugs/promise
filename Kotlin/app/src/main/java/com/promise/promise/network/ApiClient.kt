package com.promise.promise.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://3.35.100.28"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val signUpService: SignUpService = retrofit.create(SignUpService::class.java)

    // API 서비스 인터페이스를 생성하는 메서드
    fun <T> createService(service: Class<T>): T {
        return retrofit.create(service)
    }
}
