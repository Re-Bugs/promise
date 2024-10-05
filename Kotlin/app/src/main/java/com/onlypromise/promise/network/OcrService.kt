// OcrService.kt
package com.onlypromise.promise.network

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface OcrService {
    @Multipart
    @POST("api/ocr/extract-text")
    fun uploadPrescriptionImage(
        @Query("bottleId") bottleId: String,
        @Part file: MultipartBody.Part
    ): Call<Void>
}