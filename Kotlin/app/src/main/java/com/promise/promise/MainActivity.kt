package com.promise.promise

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.promise.promise.network.ApiClient
import com.promise.promise.network.OcrService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var selectedImageUri: Uri
    private lateinit var bottleCode: String
    private lateinit var resultLabel: TextView
    private lateinit var photoFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 이전에 저장된 약통코드 불러오기
        loadBottleCode()

        // 결과를 표시할 라벨 초기화
        resultLabel = findViewById(R.id.resultLabel)

        // 삭제 버튼 초기화 및 클릭 이벤트 설정
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            deleteStoredData() // 저장된 이름과 약통 코드 삭제
            navigateToSplashActivity() // SplashActivity로 이동
        }

        // 처방전 인식 버튼 초기화 및 클릭 이벤트 설정
        val ocrButton = findViewById<Button>(R.id.ocrButton)
        ocrButton.setOnClickListener {
            openGallery()
        }

        // 처방전 촬영 버튼 초기화 및 클릭 이벤트 설정
        val cameraButton = findViewById<Button>(R.id.cameraButton)
        cameraButton.setOnClickListener {
            openCamera()
        }
    }

    // 이전에 저장된 약통코드 불러오는 메서드
    private fun loadBottleCode() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        bottleCode = sharedPref.getString("bottleCode", "") ?: ""
    }

    // 갤러리를 열어 이미지를 선택하는 메서드
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    // 갤러리에서 선택된 이미지를 처리하는 콜백
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                val realPath = getRealPathFromURI(uri) // 실제 파일 경로 변환
                if (realPath != null) {
                    sendPrescriptionImage(Uri.fromFile(File(realPath)))
                } else {
                    resultLabel.text = "이미지 파일을 찾을 수 없습니다."
                }
            }
        } else {
            resultLabel.text = "이미지 선택이 취소되었습니다."
        }
    }

    // 카메라를 열어 사진을 촬영하는 메서드
    private fun openCamera() {
        try {
            photoFile = createImageFile()
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            cameraLauncher.launch(cameraIntent)
        } catch (ex: IOException) {
            resultLabel.text = "사진 파일 생성에 실패했습니다."
        } catch (ex: IllegalArgumentException) {
            resultLabel.text = "파일 경로 설정 오류가 발생했습니다."
        }
    }

    // 사진 파일 생성 메서드
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    // 카메라에서 촬영된 이미지를 처리하는 콜백
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = Uri.fromFile(photoFile)
            sendPrescriptionImage(selectedImageUri)
        } else {
            resultLabel.text = "사진 촬영이 취소되었습니다."
        }
    }

    // 이미지 파일을 서버로 전송하는 메서드
    private fun sendPrescriptionImage(uri: Uri) {
        val file = File(uri.path ?: "")
        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val call = ApiClient.createService(OcrService::class.java)
            .uploadPrescriptionImage(bottleCode, body)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    resultLabel.text = "처방전 이미지 전송 성공!"
                    scanFile(this@MainActivity, file.absolutePath)
                } else {
                    resultLabel.text = "이미지 전송 실패: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                resultLabel.text = "네트워크 오류: ${t.message}"
            }
        })
    }

    // 미디어 스캐너를 호출하여 갤러리에 파일을 표시하도록 하는 메서드
    private fun scanFile(context: Context, path: String) {
        val file = File(path)
        val uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
        context.sendBroadcast(intent)
    }

    // SharedPreferences에 저장된 이름과 약통 코드를 삭제하는 메서드
    private fun deleteStoredData() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("userName")
            remove("bottleCode")
            putBoolean("isRegistered", false)
            apply()
        }
    }

    // SplashActivity로 이동하는 메서드
    private fun navigateToSplashActivity() {
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }

    // URI에서 실제 파일 경로를 얻는 메서드
    private fun getRealPathFromURI(uri: Uri): String? {
        var filePath: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                filePath = it.getString(columnIndex)
            }
        }
        return filePath
    }
}