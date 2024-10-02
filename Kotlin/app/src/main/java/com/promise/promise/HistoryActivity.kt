package com.promise.promise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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

class HistoryActivity : AppCompatActivity() {

    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private var notificationList = mutableListOf<NotificationItem>()
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // RecyclerView 설정
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView)
        notificationRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationAdapter = NotificationAdapter(notificationList)
        notificationRecyclerView.adapter = notificationAdapter

        // emptyView 설정
        emptyView = findViewById(R.id.emptyView)

        // API 호출하여 데이터 가져오기
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

    private fun loadNotifications() {
        val notificationService = ApiClient.createService(NotificationService::class.java)

        // SharedPreferences에서 bottleCode 가져오기
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val bottleCode = sharedPref.getString("bottleCode", "") ?: ""

        notificationService.getNotifications(bottleCode).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val notifications = response.body()?.get("notifications") as? List<Map<String, Any>>

                    if (notifications != null && notifications.isNotEmpty()) {
                        for (notification in notifications) {
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

                            Log.d("HistoryActivity", "약 이름: $name, 남은 복용량: $remainingDose, 갱신 날짜: $renewalDate, 복용 시간: ${doseTimes.joinToString(", ")}")

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
                        emptyView.visibility = View.GONE // 데이터가 있으면 emptyView 숨김
                    } else {
                        Log.d("HistoryActivity", "알림 리스트가 비어 있습니다.")
                        emptyView.visibility = View.VISIBLE // 리스트가 비어있으면 emptyView 표시
                    }
                } else {
                    Log.e("HistoryActivity", "응답 에러: ${response.code()}")
                    emptyView.text = "알림 리스트가 비어 있습니다." // 에러 메시지 표시
                    emptyView.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("HistoryActivity", "네트워크 오류: ${t.message}")
                emptyView.text = "인터넷 연결을 확인해주세요." // 네트워크 오류 메시지 표시
                emptyView.visibility = View.VISIBLE
                Toast.makeText(this@HistoryActivity, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}