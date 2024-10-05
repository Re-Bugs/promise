package com.promise.promise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.promise.promise.network.ApiClient
import com.promise.promise.network.NotificationService
import com.promise.promise.utils.AlarmManagerUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsActivity : AppCompatActivity() {

    private lateinit var notificationSpinner: Spinner
    private lateinit var updateButton: Button
    private lateinit var resetButton: Button

    // 알림 시간 Picker
    private lateinit var morningTimePicker: TimePicker
    private lateinit var afternoonTimePicker: TimePicker
    private lateinit var eveningTimePicker: TimePicker
    private lateinit var setAlarmTimesButton: Button
    private var currentNotificationValue: String = "none" // 현재 알림 설정 값 저장

    // SharedPreferences에서 저장된 bottleCode를 불러오기
    private fun getStoredBottleCode(): String {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return sharedPref.getString("bottleCode", "") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // View 초기화
        notificationSpinner = findViewById(R.id.notificationSpinner)
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

        // 시간 제한 설정
        setTimeRestrictions()

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
        val adapter = ArrayAdapter(this, R.layout.spinner_item, notificationOptions)
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

    // 시간 제한 설정 메서드
    private fun setTimeRestrictions() {
        morningTimePicker.setOnTimeChangedListener { _, hourOfDay, _ ->
            if (hourOfDay !in 4..10) {
                morningTimePicker.hour = 4
                morningTimePicker.minute = 1
                Toast.makeText(this, "오전 4시 1분에서 오전 10시까지만 선택 가능합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        afternoonTimePicker.setOnTimeChangedListener { _, hourOfDay, _ ->
            if (hourOfDay !in 10..15 || (hourOfDay == 10 && afternoonTimePicker.minute == 0)) {
                afternoonTimePicker.hour = 10
                afternoonTimePicker.minute = 1
                Toast.makeText(this, "오전 10시 1분에서 오후 3시까지만 선택 가능합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        eveningTimePicker.setOnTimeChangedListener { _, hourOfDay, _ ->
            if ((hourOfDay in 0..3) || (hourOfDay !in 15..23)) {
                eveningTimePicker.hour = 15
                eveningTimePicker.minute = 1
                Toast.makeText(this, "오후 3시 1분에서 오전 4시까지만 선택 가능합니다.", Toast.LENGTH_SHORT).show()
            }
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
                Log.e("SettingActivity", "네트워크 오류: ${t.message}", t)
                Toast.makeText(this@SettingsActivity, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // PATCH 요청: 알림 시간 업데이트 후 알람 재설정
    private fun updateAlarmTimes(bottleCode: String) {
        val morningTime = formatTime(morningTimePicker.hour, morningTimePicker.minute)
        val afternoonTime = formatTime(afternoonTimePicker.hour, afternoonTimePicker.minute)
        val eveningTime = formatTime(eveningTimePicker.hour, eveningTimePicker.minute)

        Log.d("RequestData", "Formatted Times - Morning: $morningTime, Afternoon: $afternoonTime, Evening: $eveningTime")

        val notificationService = ApiClient.createService(NotificationService::class.java)
        notificationService.updateAlarmTimes(bottleCode, morningTime, afternoonTime, eveningTime).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("ServerResponse", "Alarm update result: $result")
                    if (result?.get("message") == "success") {
                        // 알림 값에 따른 알람 처리
                        notificationService.getNotifications(bottleCode).enqueue(object : Callback<Map<String, Any>> {
                            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                                if (response.isSuccessful) {
                                    val notifications = response.body()?.get("notifications") as? List<Map<String, Any>>
                                    var morningSet = false
                                    var afternoonSet = false
                                    var eveningSet = false

                                    notifications?.forEach { notification ->
                                        if (notification["morning"] == true && !morningSet) {
                                            AlarmManagerUtils.manageAlarm(this@SettingsActivity, morningTime, "morning", true)
                                            morningSet = true // 중복 설정 방지
                                            Log.d("AlarmManager", "morning 알람이 설정되었습니다.")
                                        }

                                        if (notification["afternoon"] == true && !afternoonSet) {
                                            AlarmManagerUtils.manageAlarm(this@SettingsActivity, afternoonTime, "afternoon", true)
                                            afternoonSet = true // 중복 설정 방지
                                            Log.d("AlarmManager", "afternoon 알람이 설정되었습니다.")
                                        }

                                        if (notification["evening"] == true && !eveningSet) {
                                            AlarmManagerUtils.manageAlarm(this@SettingsActivity, eveningTime, "evening", true)
                                            eveningSet = true // 중복 설정 방지
                                            Log.d("AlarmManager", "evening 알람이 설정되었습니다.")
                                        }
                                    }

                                    // 만약 해당 시간대에 알람이 없다면 해당 알람을 취소
                                    if (!morningSet) {
                                        AlarmManagerUtils.manageAlarm(this@SettingsActivity, morningTime, "morning", false)
                                        Log.d("AlarmManager", "morning 알람이 취소되었습니다.")
                                    }
                                    if (!afternoonSet) {
                                        AlarmManagerUtils.manageAlarm(this@SettingsActivity, afternoonTime, "afternoon", false)
                                        Log.d("AlarmManager", "afternoon 알람이 취소되었습니다.")
                                    }
                                    if (!eveningSet) {
                                        AlarmManagerUtils.manageAlarm(this@SettingsActivity, eveningTime, "evening", false)
                                        Log.d("AlarmManager", "evening 알람이 취소되었습니다.")
                                    }
                                } else {
                                    Toast.makeText(this@SettingsActivity, "알람 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                                Log.e("SettingActivity", "네트워크 오류: ${t.message}", t)
                                Toast.makeText(this@SettingsActivity, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
                            }
                        })

                        Toast.makeText(this@SettingsActivity, "알림 시간이 변경되었고 알람이 설정되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity, "알림 시간 변경 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val statusCode = response.code()
                    Toast.makeText(this@SettingsActivity, "서버 오류: $errorBody", Toast.LENGTH_SHORT).show()
                    Log.e("ServerError", "Failed to update alarm times. Status code: $statusCode, Error: $errorBody")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Toast.makeText(this@SettingsActivity, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
                Log.e("NetworkError", "Network failure: ${t.message}")
            }
        })
    }

    // 시간 포맷 함수
    private fun formatTime(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }


    // GET 요청: 알림 설정 가져오기
    private fun getNotificationValue(bottleCode: String) {
        val notificationService = ApiClient.createService(NotificationService::class.java)
        notificationService.getNotificationValue(bottleCode).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    val notificationValue = response.body()?.get("NotificationValue")
                    notificationSpinner.setSelection((notificationSpinner.adapter as ArrayAdapter<String>).getPosition(notificationValue))
                    currentNotificationValue = notificationValue ?: "none"  // 알림 값 저장
                } else {
                    Toast.makeText(this@SettingsActivity, "알림 값을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("SettingActivity", "네트워크 오류: ${t.message}", t)
                Toast.makeText(this@SettingsActivity, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
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

                    if (value == "bottle" || value == "none") {
                        AlarmManagerUtils.manageAlarm(this@SettingsActivity, formatTime(morningTimePicker.hour, morningTimePicker.minute), "morning", false)
                        AlarmManagerUtils.manageAlarm(this@SettingsActivity, formatTime(afternoonTimePicker.hour, afternoonTimePicker.minute), "afternoon", false)
                        AlarmManagerUtils.manageAlarm(this@SettingsActivity, formatTime(eveningTimePicker.hour, eveningTimePicker.minute), "evening", false)
                        Toast.makeText(this@SettingsActivity, "기존 알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    } else if (value == "app" || value == "mix") {
                        val morningTime = formatTime(morningTimePicker.hour, morningTimePicker.minute)
                        val afternoonTime = formatTime(afternoonTimePicker.hour, afternoonTimePicker.minute)
                        val eveningTime = formatTime(eveningTimePicker.hour, eveningTimePicker.minute)
                        AlarmManagerUtils.manageAlarm(this@SettingsActivity, morningTime, "morning", true)
                        AlarmManagerUtils.manageAlarm(this@SettingsActivity, afternoonTime, "afternoon", true)
                        AlarmManagerUtils.manageAlarm(this@SettingsActivity, eveningTime, "evening", true)
                        Toast.makeText(this@SettingsActivity, "알람이 설정되었습니다.", Toast.LENGTH_SHORT).show()
                    }

                    currentNotificationValue = value // 알림 값 저장

                } else {
                    Toast.makeText(this@SettingsActivity, "알림 설정 변경 실패: ${result?.get("message")}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("SettingActivity", "네트워크 오류: ${t.message}", t)
                Toast.makeText(this@SettingsActivity, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
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
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("userName")
            remove("bottleCode")
            putBoolean("isLoggedIn", false)
            apply()
        }
    }
}