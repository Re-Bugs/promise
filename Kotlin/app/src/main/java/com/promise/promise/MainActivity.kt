package com.promise.promise

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.promise.promise.network.ApiClient
import com.promise.promise.network.NotificationService
import com.promise.promise.network.OcrService
import com.promise.promise.utils.AlarmManagerUtils // AlarmManagerUtils 임포트
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var selectedImageUri: Uri
    private lateinit var bottleCode: String
    private lateinit var resultLabel: TextView
    private lateinit var notificationValueLabel: TextView
    private lateinit var alarmTimesLabel: TextView
    private lateinit var photoFile: File
    private var currentNotificationValue: String = "none" // 현재 알림 설정 값 저장

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 알림 권한 요청
        requestNotificationPermission()

        // 이전에 저장된 약통코드 불러오기
        loadBottleCode()

        // 결과를 표시할 라벨 초기화
        resultLabel = findViewById(R.id.resultLabel)
        notificationValueLabel = findViewById(R.id.notificationValueLabel)
        alarmTimesLabel = findViewById(R.id.alarmTimesLabel)

        // 알림 정보를 불러오기
        getNotificationValue()
        getAlarmTimes()

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

        // 네비게이션 바 초기화 및 클릭 이벤트 설정
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_home
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_notification -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.navigation_setting -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    // 알림 권한 요청 함수
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e("MainActivity", "알림 권한이 거부되었습니다.")
            }
        }
    }

    private fun loadBottleCode() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        bottleCode = sharedPref.getString("bottleCode", "") ?: ""
    }

    // 알림 시간 정보 불러오기
    private fun getAlarmTimes() {
        val notificationService = ApiClient.createService(NotificationService::class.java)
        notificationService.getAlarmTimes(bottleCode).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    val alarmTimes = response.body()
                    alarmTimes?.let {
                        val morning = it["morning"] ?: "없음"
                        val afternoon = it["afternoon"] ?: "없음"
                        val evening = it["evening"] ?: "없음"
                        alarmTimesLabel.text = "아침 알림 시간: $morning\n점심 알림 시간: $afternoon\n저녁 알림 시간: $evening"

                        // 알림 값에 따라 알람을 설정하거나 취소
                        if (currentNotificationValue == "app" || currentNotificationValue == "mix") {
                            AlarmManagerUtils.manageAlarm(this@MainActivity, morning, "morning", morning != "없음")
                            AlarmManagerUtils.manageAlarm(this@MainActivity, afternoon, "afternoon", afternoon != "없음")
                            AlarmManagerUtils.manageAlarm(this@MainActivity, evening, "evening", evening != "없음")
                        } else if (currentNotificationValue == "bottle" || currentNotificationValue == "none") {
                            // 기존 알람 모두 취소
                            AlarmManagerUtils.manageAlarm(this@MainActivity, morning, "morning", false)
                            AlarmManagerUtils.manageAlarm(this@MainActivity, afternoon, "afternoon", false)
                            AlarmManagerUtils.manageAlarm(this@MainActivity, evening, "evening", false)
                        }
                    }
                } else {
                    alarmTimesLabel.text = "알림 시간을 불러오지 못했습니다."
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("MainActivity", "네트워크 오류: ${t.message}", t)
                alarmTimesLabel.text = "인터넷 연결을 확인해주세요."
            }
        })
    }

    // 알림 유형 정보 불러오기
    private fun getNotificationValue() {
        val notificationService = ApiClient.createService(NotificationService::class.java)
        notificationService.getNotificationValue(bottleCode).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    val notificationValue = response.body()?.get("NotificationValue") ?: "없음"
                    notificationValueLabel.text = "알림 유형: $notificationValue"
                    currentNotificationValue = notificationValue  // 알림 값을 저장하여 알람 설정에 사용
                } else {
                    notificationValueLabel.text = "알림 값을 불러오지 못했습니다."
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("MainActivity", "네트워크 오류: ${t.message}", t)
                notificationValueLabel.text = "인터넷 연결을 확인해주세요."
            }
        })
    }

    // 이하 기존의 openGallery, openCamera 등의 코드 유지

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                val realPath = getRealPathFromURI(this, uri)
                if (realPath != null) {
                    sendPrescriptionImage(Uri.fromFile(File(realPath)))
                } else {
                    resultLabel.text = "이미지 파일 경로를 찾을 수 없습니다."
                }
            }
        } else {
            resultLabel.text = "이미지 선택이 취소되었습니다."
        }
    }

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
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = Uri.fromFile(photoFile)
            sendPrescriptionImage(selectedImageUri)
        } else {
            resultLabel.text = "사진 촬영이 취소되었습니다."
        }
    }

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
                } else {
                    resultLabel.text = "이미지 전송 실패: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("MainActivity", "네트워크 오류: ${t.message}", t)
                resultLabel.text = "인터넷 연결을 확인해주세요."
            }
        })
    }

    private fun getRealPathFromURI(context: Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        if (cursor != null) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            path = cursor.getString(columnIndex)
            cursor.close()
        }
        return path
    }
}