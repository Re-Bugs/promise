package com.onlypromise.promise.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ReportService {
    @Multipart
    @POST("/api/addReport")
    fun addReport(
        @Part("reportDTO") reportDTO: RequestBody,
        @Part imageFile: MultipartBody.Part? = null
    ): Call<Map<String, String>>
}