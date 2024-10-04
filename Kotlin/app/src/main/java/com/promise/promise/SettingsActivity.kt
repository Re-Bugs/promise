package com.promise.promise

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.promise.promise.network.ApiClient
import com.promise.promise.network.NotificationService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

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
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
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
                        if (currentNotificationValue == "app" || currentNotificationValue == "mix") {
                            // 알람 설정
                            setAlarm(morningTime, "morning")
                            setAlarm(afternoonTime, "afternoon")
                            setAlarm(eveningTime, "evening")
                            Toast.makeText(this@SettingsActivity, "알림 시간이 변경되었고 알람이 설정되었습니다.", Toast.LENGTH_SHORT).show()
                        } else if (currentNotificationValue == "bottle" || currentNotificationValue == "none") {
                            // 기존 알람 취소
                            cancelAlarm("morning")
                            cancelAlarm("afternoon")
                            cancelAlarm("evening")
                            Toast.makeText(this@SettingsActivity, "알림 시간이 변경되었고 기존 알람이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                        }
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

    // 알람 설정 함수
    private fun setAlarm(time: String, type: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarmType", type)
            putExtra("alarmTime", time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 시간을 "HH:mm" 형식으로 받아와 파싱하여 알람을 설정
        val timeParts = time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // 현재 시간보다 이전 시간일 경우 다음날로 설정
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            Log.d("MainActivity", "$type 알람이 설정되었습니다: ${calendar.time}")
        } catch (e: SecurityException) {
            AlertDialog.Builder(this)
                .setTitle("권한 부족")
                .setMessage("정확한 알람을 설정할 권한이 없습니다. 설정에서 권한을 활성화해 주세요.")
                .setPositiveButton("설정으로 이동") { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    // 알람 취소 함수
    private fun cancelAlarm(type: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("MainActivity", "$type 알람이 취소되었습니다.")
    }

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

                    // 'bottle' 또는 'none'일 경우 기존 알람 삭제
                    if (value == "bottle" || value == "none") {
                        cancelAlarm("morning")
                        cancelAlarm("afternoon")
                        cancelAlarm("evening")
                        Toast.makeText(this@SettingsActivity, "기존 알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    } else if (value == "app" || value == "mix") {
                        // 알람이 다시 설정되도록 함
                        val morningTime = formatTime(morningTimePicker.hour, morningTimePicker.minute)
                        val afternoonTime = formatTime(afternoonTimePicker.hour, afternoonTimePicker.minute)
                        val eveningTime = formatTime(eveningTimePicker.hour, eveningTimePicker.minute)
                        setAlarm(morningTime, "morning")
                        setAlarm(afternoonTime, "afternoon")
                        setAlarm(eveningTime, "evening")
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
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("userName")
            remove("bottleCode")
            putBoolean("isLoggedIn", false)
            apply()
        }
    }
}