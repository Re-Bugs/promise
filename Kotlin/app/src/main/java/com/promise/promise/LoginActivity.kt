package com.promise.promise

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.promise.promise.network.ApiClient
import com.promise.promise.network.LoginRequest
import com.promise.promise.network.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var bottleCodeEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var ageEditText: EditText

    private val PERMISSION_REQUEST_CODE = 100
    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // EditText 및 버튼 초기화
        bottleCodeEditText = findViewById(R.id.bottleCodeEditText)
        nameEditText = findViewById(R.id.nameEditText)
        ageEditText = findViewById(R.id.ageEditText)
        setupLoginButton()

        // EditText에 포커스가 설정되면 키보드를 띄우도록 설정
        showKeyboardOnFocus(nameEditText)
        showKeyboardOnFocus(bottleCodeEditText)
        showKeyboardOnFocus(ageEditText)

        // 알림 권한 요청
        requestNotificationPermission()

        // 카메라 권한 요청 (이 부분 추가)
        requestCameraPermission()
    }

    // 알림 권한 요청 함수
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("LoginActivity", "알림 권한이 없으므로 요청합니다.")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                Log.d("LoginActivity", "알림 권한이 이미 허용되어 있습니다.")
            }
        }
    }

    // 카메라 권한 요청 함수 추가
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LoginActivity", "카메라 권한이 없으므로 요청합니다.")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            Log.d("LoginActivity", "카메라 권한이 이미 허용되어 있습니다.")
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("LoginActivity", "알림 권한이 허용되었습니다.")
                } else {
                    Log.e("LoginActivity", "알림 권한이 거부되었습니다.")
                }
            }
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("LoginActivity", "카메라 권한이 허용되었습니다.")
                } else {
                    Log.e("LoginActivity", "카메라 권한이 거부되었습니다.")
                }
            }
        }
    }

    // EditText 선택 시 키보드를 자동으로 띄우는 메서드
    private fun showKeyboardOnFocus(editText: EditText) {
        editText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                // 키보드를 강제로 띄움
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    // 로그인 버튼 클릭 이벤트 설정
    private fun setupLoginButton() {
        val loginButton = findViewById<Button>(R.id.loginButton)
        loginButton.setOnClickListener {
            val bottleCode = bottleCodeEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val age = ageEditText.text.toString().trim()

            if (isValidName(name) && isValidBottleCode(bottleCode) && isValidAge(age)) {
                // 로그인 요청 보내기
                sendLoginRequest(name, age, bottleCode)
            }
        }
    }

    // 이름 유효성 검사
    private fun isValidName(name: String): Boolean {
        return if (name.isEmpty()) {
            nameEditText.error = "이름을 입력하세요."
            false
        } else if (name.length > 5) {
            nameEditText.error = "이름은 5글자 이하여야 합니다."
            false
        } else {
            true
        }
    }

    // 나이 유효성 검사
    private fun isValidAge(age: String): Boolean {
        return if (age.isEmpty()) {
            ageEditText.error = "나이를 입력하세요."
            false
        } else if (!age.matches(Regex("^[0-9]{1,3}$"))) {
            ageEditText.error = "유효한 나이를 입력하세요."
            false
        } else {
            true
        }
    }

    // 약통 코드 유효성 검사
    private fun isValidBottleCode(bottleCode: String): Boolean {
        val regex = Regex("^[a-z0-9]{5}$") // 소문자와 숫자 5글자 패턴
        return if (bottleCode.isEmpty()) {
            bottleCodeEditText.error = "약통 코드를 입력하세요."
            false
        } else if (!bottleCode.matches(regex)) {
            bottleCodeEditText.error = "약통 코드는 소문자와 숫자로 이루어진 5글자여야 합니다."
            false
        } else {
            true
        }
    }

    // 로그인 요청을 보내는 메서드
    private fun sendLoginRequest(name: String, age: String, bottleCode: String) {
        val request = LoginRequest(name, age.toInt(), bottleCode)

        // Retrofit을 사용하여 서버로 로그인 요청 전송
        ApiClient.loginService.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.message == "success") {
                        Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        saveLoginStatus(name, age, bottleCode) // 이름, 나이, 약통 코드 저장
                        navigateToMainActivity() // 메인 화면으로 이동
                    } else {
                        Toast.makeText(this@LoginActivity, "로그인 실패: ${result?.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("LoginActivity", "서버 오류: ${response.code()}, 오류 메시지: ${response.errorBody()?.string()}")
                    Toast.makeText(this@LoginActivity, "서버 오류: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // 네트워크 오류 발생 시 로그 출력
                Log.e("LoginActivity", "네트워크 오류: ${t.message}", t)
                Toast.makeText(this@LoginActivity, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 로그인 성공 시 상태와 이름, 나이, 약통 코드를 SharedPreferences에 저장하는 메서드
    private fun saveLoginStatus(name: String, age: String, bottleCode: String) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isLoggedIn", true)
            putString("userName", name) // 입력된 이름 저장
            putString("userAge", age) // 입력된 나이 저장
            putString("bottleCode", bottleCode) // 입력된 약통 코드 저장
            apply() // 데이터를 비동기적으로 저장
        }
    }

    // 메인 화면으로 이동하는 메서드
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // 현재 Activity 종료
    }
}