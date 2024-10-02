package com.promise.promise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.promise.promise.network.ApiClient
import com.promise.promise.network.NotificationService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var takenRecyclerView: RecyclerView
    private lateinit var notTakenRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var takenAdapter: MedicineAdapter
    private lateinit var notTakenAdapter: MedicineAdapter
    private var notificationList = mutableListOf<NotificationItem>()
    private var takenList = mutableListOf<MedicineItem>()
    private var notTakenList = mutableListOf<MedicineItem>()
    private lateinit var emptyView: TextView
    private lateinit var selectedDateTextView: TextView
    private lateinit var previousDayButton: ImageView
    private lateinit var nextDayButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // RecyclerView 설정
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView)
        takenRecyclerView = findViewById(R.id.takenRecyclerView)
        notTakenRecyclerView = findViewById(R.id.notTakenRecyclerView)

        notificationRecyclerView.layoutManager = LinearLayoutManager(this)
        takenRecyclerView.layoutManager = LinearLayoutManager(this)
        notTakenRecyclerView.layoutManager = LinearLayoutManager(this)

        notificationAdapter = NotificationAdapter(notificationList)
        takenAdapter = MedicineAdapter(takenList)
        notTakenAdapter = MedicineAdapter(notTakenList)

        notificationRecyclerView.adapter = notificationAdapter
        takenRecyclerView.adapter = takenAdapter
        notTakenRecyclerView.adapter = notTakenAdapter

        // emptyView 설정
        emptyView = findViewById(R.id.emptyView)

        // 날짜 선택 TextView 및 화살표 버튼 설정
        selectedDateTextView = findViewById(R.id.selectedDate)
        previousDayButton = findViewById(R.id.previousDayButton)
        nextDayButton = findViewById(R.id.nextDayButton)

        val calendar = Calendar.getInstance()

        previousDayButton.setOnClickListener {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            updateSelectedDate(calendar)
        }

        nextDayButton.setOnClickListener {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            updateSelectedDate(calendar)
        }

        // 기본적으로 오늘 날짜 표시 및 데이터 로드
        updateSelectedDate(calendar)

        // 알림 리스트와 복약 상태 데이터 로드
        loadNotifications()

        // 하단 네비게이션 바 설정
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_notification
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_notification -> true
                R.id.navigation_setting -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun updateSelectedDate(calendar: Calendar) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDate = dateFormat.format(calendar.time)
        selectedDateTextView.text = selectedDate
        loadDailyTaken(selectedDate)
    }

    private fun loadDailyTaken(date: String) {
        val notificationService = ApiClient.createService(NotificationService::class.java)

        // SharedPreferences에서 bottleCode 가져오기
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val bottleCode = sharedPref.getString("bottleCode", "") ?: ""

        notificationService.getDailyTaken(bottleCode, date).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val notTaken = response.body()?.get("notTaken") as? List<Map<String, Any>>
                    val taken = response.body()?.get("taken") as? List<Map<String, Any>>

                    takenList.clear()
                    notTakenList.clear()

                    notTaken?.forEach { item ->
                        notTakenList.add(
                            MedicineItem(
                                id = (item["id"] as Double).toInt(),
                                medicineName = item["medicineName"] as String,
                                isTaken = false
                            )
                        )
                    }

                    taken?.forEach { item ->
                        takenList.add(
                            MedicineItem(
                                id = (item["id"] as Double).toInt(),
                                medicineName = item["medicineName"] as String,
                                isTaken = true
                            )
                        )
                    }

                    takenAdapter.notifyDataSetChanged()
                    notTakenAdapter.notifyDataSetChanged()

                    emptyView.visibility = if (takenList.isEmpty() && notTakenList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Log.e("HistoryActivity", "응답 에러: ${response.code()}")
                    emptyView.text = "복약 상태를 불러올 수 없습니다."
                    emptyView.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("HistoryActivity", "네트워크 오류: ${t.message}")
                emptyView.text = "인터넷 연결을 확인해주세요."
                emptyView.visibility = View.VISIBLE
                Toast.makeText(this@HistoryActivity, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadNotifications() {
        val notificationService = ApiClient.createService(NotificationService::class.java)

        // SharedPreferences에서 bottleCode 가져오기
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val bottleCode = sharedPref.getString("bottleCode", "") ?: ""

        notificationService.getNotifications(bottleCode).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val notifications = response.body()?.get("notifications") as? List<Map<String, Any>>

                    notificationList.clear()

                    notifications?.forEach { notification ->
                        val name = notification["name"] as String
                        val remainingDose = (notification["remainingDose"] as Double).toInt()
                        val renewalDate = notification["renewalDate"] as String
                        val morning = notification["morning"] as Boolean
                        val afternoon = notification["afternoon"] as Boolean
                        val evening = notification["evening"] as Boolean

                        val doseTimes = mutableListOf<String>()
                        if (morning) doseTimes.add("아침")
                        if (afternoon) doseTimes.add("점심")
                        if (evening) doseTimes.add("저녁")

                        notificationList.add(
                            NotificationItem(
                                id = (notification["id"] as Double).toInt(),
                                name = name,
                                remainingDose = remainingDose,
                                renewalDate = renewalDate,
                                doseTimes = doseTimes.joinToString(", ")
                            )
                        )
                    }

                    notificationAdapter.notifyDataSetChanged()
                } else {
                    Log.e("HistoryActivity", "알림 리스트를 불러올 수 없습니다.")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("HistoryActivity", "네트워크 오류: ${t.message}")
            }
        })
    }
}