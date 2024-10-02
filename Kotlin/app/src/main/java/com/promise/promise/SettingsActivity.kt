package com.promise.promise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.promise.promise.network.ApiClient
import com.promise.promise.network.NotificationService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsActivity : AppCompatActivity() {

    private lateinit var notificationSpinner: Spinner
    private lateinit var currentNotificationTextView: TextView
    private lateinit var updateButton: Button
    private lateinit var resetButton: Button

    // 알림 시간 Picker
    private lateinit var morningTimePicker: TimePicker
    private lateinit var afternoonTimePicker: TimePicker
    private lateinit var eveningTimePicker: TimePicker
    private lateinit var setAlarmTimesButton: Button

    // SharedPreferences에서 저장된 bottleCode를 불러오기
    private fun getStoredBottleCode(): String {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("bottleCode", "") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // View 초기화
        notificationSpinner = findViewById(R.id.notificationSpinner)
        currentNotificationTextView = findViewById(R.id.currentNotificationTextView)
        updateButton = findViewById(R.id.updateButton)
        resetButton = findViewById(R.id.resetButton)
        setAlarmTimesButton = findViewById(R.id.setAlarmTimesButton)

        // 알림 시간 Picker 초기화
        morningTimePicker = findViewById(R.id.morningTimePicker)
        afternoonTimePicker = findViewById(R.id.afternoonTimePicker)
        eveningTimePicker = findViewById(R.id.eveningTimePicker)

        // TimePicker 24시간 형식 설정
        morningTimePicker.setIs24HourView(true)
        afternoonTimePicker.setIs24HourView(true)
        eveningTimePicker.setIs24HourView(true)

        // 애플리케이션 초기화 버튼 클릭 이벤트 설정
        resetButton.setOnClickListener {
            resetApplication()
        }

        // 하단 네비게이션 바 설정
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_setting

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_notification -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.navigation_setting -> true
                else -> false
            }
        }

        // 알림 선택 Spinner에 값 설정
        val notificationOptions = arrayOf("bottle", "app", "mix", "none")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, notificationOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        notificationSpinner.adapter = adapter

        // 저장된 bottleCode 불러오기
        val bottleCode = getStoredBottleCode()

        // 알림 설정 값을 서버에서 불러오기
        getNotificationValue(bottleCode)

        // 알림 시간 불러오기
        getAlarmTimes(bottleCode)

        // 알림 설정 업데이트
        updateButton.setOnClickListener {
            val selectedValue = notificationSpinner.selectedItem.toString()
            updateNotificationValue(bottleCode, selectedValue)
        }

        // 알람 시간 변경 버튼 클릭 이벤트 설정
        setAlarmTimesButton.setOnClickListener {
            updateAlarmTimes(bottleCode)
        }
    }

    // GET 요청: 알림 시간 불러오기
    private fun getAlarmTimes(bottleCode: String) {
        val notificationService = ApiClient.createService(NotificationService::class.java)
        notificationService.getAlarmTimes(bottleCode).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    val alarmTimes = response.body()
                    alarmTimes?.let {
                        val morning = it["morning"]?.split(":") ?: listOf("06", "00")
                        val afternoon = it["afternoon"]?.split(":") ?: listOf("14", "00")
                        val evening = it["evening"]?.split(":") ?: listOf("19", "30")

                        morningTimePicker.hour = morning[0].toInt()
                        morningTimePicker.minute = morning[1].toInt()

                        afternoonTimePicker.hour = afternoon[0].toInt()
                        afternoonTimePicker.minute = afternoon[1].toInt()

                        eveningTimePicker.hour = evening[0].toInt()
                        eveningTimePicker.minute = evening[1].toInt()
                    }
                } else {
                    Toast.makeText(this@SettingsActivity, "알림 시간을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Toast.makeText(this@SettingsActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // PATCH 요청: 알림 시간 업데이트
    private fun updateAlarmTimes(bottleCode: String) {
        val morningTime = formatTime(morningTimePicker.hour, morningTimePicker.minute)
        val afternoonTime = formatTime(afternoonTimePicker.hour, afternoonTimePicker.minute)
        val eveningTime = formatTime(eveningTimePicker.hour, eveningTimePicker.minute)

        Log.d("RequestData", "Formatted Times - Morning: $morningTime, Afternoon: $afternoonTime, Evening: $eveningTime") // 로그 추가

        val notificationService = ApiClient.createService(NotificationService::class.java)
        notificationService.updateAlarmTimes(bottleCode, morningTime, afternoonTime, eveningTime).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("ServerResponse", "Alarm update result: $result")
                    if (result?.get("message") == "success") {
                        Toast.makeText(this@SettingsActivity, "알림 시간이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity, "알림 시간 변경 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() // 서버에서 받은 에러 메시지 확인
                    val statusCode = response.code() // 상태 코드 확인
                    Toast.makeText(this@SettingsActivity, "서버 오류: $errorBody", Toast.LENGTH_SHORT).show()
                    Log.e("ServerError", "Failed to update alarm times. Status code: $statusCode, Error: $errorBody")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Toast.makeText(this@SettingsActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("NetworkError", "Network failure: ${t.message}")
            }
        })
    }

    // GET 요청: 알림 설정 가져오기
    private fun getNotificationValue(bottleCode: String) {
        val notificationService = ApiClient.createService(NotificationService::class.java)
        notificationService.getNotificationValue(bottleCode).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    val notificationValue = response.body()?.get("NotificationValue")
                    currentNotificationTextView.text = "현재 알림 설정: $notificationValue"
                    notificationSpinner.setSelection((notificationSpinner.adapter as ArrayAdapter<String>).getPosition(notificationValue))
                } else {
                    Toast.makeText(this@SettingsActivity, "알림 값을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Toast.makeText(this@SettingsActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // PATCH 요청: 알림 설정 업데이트
    private fun updateNotificationValue(bottleCode: String, value: String) {
        val notificationService = ApiClient.createService(NotificationService::class.java)
        notificationService.updateNotificationValue(bottleCode, value).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                val result = response.body()
                if (response.isSuccessful && result?.get("message") == "success") {
                    Toast.makeText(this@SettingsActivity, "알림 설정이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "알림 설정 변경 실패: ${result?.get("message")}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Toast.makeText(this@SettingsActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 애플리케이션 초기화 메서드
    private fun resetApplication() {
        deleteStoredData()
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }

    // 저장된 데이터 삭제
    private fun deleteStoredData() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("userName")
            remove("bottleCode")
            putBoolean("isLoggedIn", false)
            apply()
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute) // 시간과 분을 두 자리로 맞춘다
    }
}