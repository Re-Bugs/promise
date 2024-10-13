package com.onlypromise.promise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.onlypromise.promise.network.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class ReportActivity : AppCompatActivity() {

    private lateinit var etSubject: EditText
    private lateinit var etContent: EditText
    private lateinit var btnAttachImage: Button
    private lateinit var btnSubmit: Button
    private lateinit var btnBack: Button
    private var imageUri: Uri? = null
    private var bottleCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        loadBottleCode()

        etSubject = findViewById(R.id.etSubject)
        etContent = findViewById(R.id.etContent)
        btnAttachImage = findViewById(R.id.btnAttachImage)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnBack = findViewById(R.id.btnBack)

        btnAttachImage.setOnClickListener { openGallery() }
        btnSubmit.setOnClickListener { submitReport() }
        btnBack.setOnClickListener { navigateBack() }
    }

    private fun loadBottleCode() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        bottleCode = sharedPref.getString("bottleCode", "") ?: ""
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            Toast.makeText(this, "이미지 파일이 선택되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitReport() {
        val subject = etSubject.text.toString()
        val content = etContent.text.toString()

        // 제목과 내용 길이 검증
        if (subject.length < 2 || subject.length > 20) {
            Toast.makeText(this, "제목은 2글자 이상 20글자 이하로 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }

        if (content.length < 5) {
            Toast.makeText(this, "내용은 5글자 이상 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }

        val reportDTO = ReportDTO(bottleId = bottleCode, title = subject, content = content)
        val gson = Gson()
        val reportJson = gson.toJson(reportDTO)
        val reportBody = RequestBody.create("application/json".toMediaTypeOrNull(), reportJson)

        val imagePart: MultipartBody.Part? = imageUri?.let {
            val filePath = getFileFromUri(it)
            if (filePath.isNotBlank()) {
                val file = File(filePath)
                val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                MultipartBody.Part.createFormData("imageFile", file.name, requestFile)
            } else null
        }

        ApiClient.reportService.addReport(reportBody, imagePart)
            .enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    val message = response.body()?.get("message") ?: "Unknown error"
                    if (message == "success") {
                        Toast.makeText(this@ReportActivity, "제출이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        navigateToMainActivity()
                    } else {
                        Toast.makeText(this@ReportActivity, "제출 실패: $message", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Toast.makeText(this@ReportActivity, "제출 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // 현재 액티비티를 종료하여 뒤로가기 시 돌아오지 않게 함
    }

    private fun getFileFromUri(uri: Uri): String {
        var filePath = ""
        val cursor: Cursor? = contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                filePath = it.getString(columnIndex)
            }
        }
        return filePath
    }

    private fun navigateBack() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
    }

    data class ReportDTO(val bottleId: String, val title: String, val content: String)
}