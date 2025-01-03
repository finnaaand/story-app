package com.example.mystory.view

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.mystory.R
import com.example.mystory.api.ApiConfig
import com.example.mystory.api.ApiService
import com.example.mystory.data.session.SessionManager
import com.example.mystory.data.login.LoginResponse
import com.example.mystory.data.session.SessionRepository
import com.example.mystory.helper.EmailEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private lateinit var sessionRepository: SessionRepository

    private lateinit var btnLogin: Button
    private lateinit var emailInput: EmailEditText
    private lateinit var passwordInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)
        sessionRepository = SessionRepository(sessionManager)
        apiService = ApiConfig.apiService

        emailInput = findViewById(R.id.ed_login_email)
        passwordInput = findViewById(R.id.ed_login_password)
        btnLogin = findViewById(R.id.btn_login)
        val tvRegister = findViewById<TextView>(R.id.tv_register)

        emailInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                emailInput.validateEmail()
            }
        }

        btnLogin.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (emailInput.validateEmail() && password.isNotEmpty()) {
                animateButtonColor()
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                tvRegister,
                "registerText"
            )
            startActivity(intent, options.toBundle())
        }
    }

    private fun animateButtonColor() {
        val colorFrom = Color.parseColor("#415f91")
        val colorTo = Color.parseColor("#aac7ff")

        val colorAnimation = ObjectAnimator.ofArgb(btnLogin, "backgroundColor", colorFrom, colorTo)
        colorAnimation.duration = 50
        colorAnimation.setEvaluator(ArgbEvaluator())
        colorAnimation.start()
    }

    private fun loginUser(email: String, password: String) {
        apiService.login(email, password).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(
                call: Call<LoginResponse>, response: Response<LoginResponse>
            ) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null && !loginResponse.error) {
                        val token = loginResponse.loginResult.token

                        sessionRepository.saveUserSession(token)

                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, loginResponse?.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Login failed!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}

