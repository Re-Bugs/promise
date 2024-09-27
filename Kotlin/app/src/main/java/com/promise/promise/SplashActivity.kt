package com.promise.promise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SharedPreferences에서 회원가입 상태 확인
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isRegistered = sharedPref.getBoolean("isRegistered", false)

        // 상태에 따라 화면 전환
        if (isRegistered) {
            // 회원가입이 되어있다면 MainActivity로 이동
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // 회원가입이 안되어 있다면 RegisterActivity로 이동
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // SplashActivity 종료
        finish()
    }
}