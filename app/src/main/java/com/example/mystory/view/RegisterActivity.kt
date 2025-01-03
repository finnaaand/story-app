package com.example.mystory.view

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.mystory.R
import com.example.mystory.api.ApiConfig
import com.example.mystory.api.ApiService
import com.example.mystory.data.RegisterResponse
import com.example.mystory.helper.EmailEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        apiService = ApiConfig.apiService

        val nameInput = findViewById<EditText>(R.id.ed_register_name)
        val emailInput = findViewById<EmailEditText>(R.id.ed_register_email)
        val passwordInput = findViewById<EditText>(R.id.ed_register_password)
        val btnRegister = findViewById<Button>(R.id.btn_register)

        btnRegister.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (password.length < 8) {
                passwordInput.error = "Password must be at least 8 characters"
                return@setOnClickListener
            }
            if (!emailInput.validateEmail()) {
                return@setOnClickListener
            }
            if (name.isNotEmpty()) {
                animateButtonColor(btnRegister)
                registerUser(name, email, password)
            } else {
                Toast.makeText(this, "Please complete all fields correctly.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun animateButtonColor(button: Button) {
        val colorFrom = Color.parseColor("#415f91")
        val colorTo = Color.parseColor("#aac7ff")

        val colorAnimation = ObjectAnimator.ofArgb(button, "backgroundColor", colorFrom, colorTo)
        colorAnimation.duration = 250
        colorAnimation.setEvaluator(ArgbEvaluator())
        colorAnimation.start()
    }

    private fun registerUser(name: String, email: String, password: String) {
        apiService.register(name, email, password).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RegisterActivity, "Register Success!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this@RegisterActivity,
                        findViewById(R.id.btn_register),
                        "registerButton"
                    )
                    startActivity(intent, options.toBundle())
                    finish()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@RegisterActivity, "Registration failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterActivity", "Error: $errorMessage")
                }
            }
            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("RegisterActivity", "Failure: ${t.message}")
            }
        })
    }
}


