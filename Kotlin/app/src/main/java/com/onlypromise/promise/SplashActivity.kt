package com.onlypromise.promise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SharedPreferences에서 로그인 상태 확인
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        // 로그인 여부에 따라 화면 전환
        if (isLoggedIn) {
            // 로그인이 되어있다면 MainActivity로 이동
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // 로그인이 안되어 있다면 LoginActivity로 이동
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // SplashActivity 종료
        finish()
    }
}