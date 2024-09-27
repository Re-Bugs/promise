package com.promise.promise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.promise.promise.network.ApiClient
import com.promise.promise.network.SignUpRequest
import com.promise.promise.network.SignUpResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var bottleCodeEditText: EditText
    private lateinit var nameEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // EditText 및 버튼 초기화
        bottleCodeEditText = findViewById(R.id.bottleCodeEditText)
        nameEditText = findViewById(R.id.nameEditText)
        setupRegisterButton()

        // EditText에 포커스가 설정되면 키보드를 띄우도록 설정
        showKeyboardOnFocus(nameEditText)
        showKeyboardOnFocus(bottleCodeEditText)

        // "이미 웹에서 가입한 아이디가 있으신가요?" 텍스트 클릭 시 LoginActivity로 이동
        val alreadyRegisteredTextView = findViewById<TextView>(R.id.alreadyRegisteredTextView)
        alreadyRegisteredTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
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

    // 버튼 클릭 이벤트를 설정하는 메서드
    private fun setupRegisterButton() {
        val registerButton = findViewById<Button>(R.id.registerButton)
        registerButton.setOnClickListener {
            val bottleCode = bottleCodeEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            if (isValidName(name) && isValidBottleCode(bottleCode)) {
                // 회원가입 요청 보내기
                sendSignUpRequest(name, bottleCode)
            }
        }
    }

    // 이름 유효성 검사: 5글자 이하인지 확인
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

    // 약통 코드 유효성 검사: 소문자와 숫자로 이루어진 5글자 조합인지 확인
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

    // 회원가입 요청을 보내는 메서드
    private fun sendSignUpRequest(name: String, bottleCode: String) {
        val request = SignUpRequest(name, bottleCode)

        // Retrofit을 사용하여 서버로 요청 전송
        ApiClient.signUpService.signUp(request).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.message == "success") {
                        Toast.makeText(this@RegisterActivity, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                        saveRegistrationStatus(name, bottleCode) // 이름과 약통 코드 저장 및 회원가입 상태 저장
                        navigateToMainActivity() // 메인 화면으로 이동
                    } else {
                        Toast.makeText(this@RegisterActivity, "회원가입 실패: ${result?.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 409 오류가 발생한 경우 처리
                    if (response.code() == 409) {
                        Toast.makeText(this@RegisterActivity, "이미 중복된 약통 코드가 존재합니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@RegisterActivity, "서버 오류: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 회원가입 완료 상태와 이름, 약통 코드를 SharedPreferences에 저장하는 메서드
    private fun saveRegistrationStatus(name: String, bottleCode: String) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isRegistered", true)
            putString("userName", name) // 입력된 이름 저장
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