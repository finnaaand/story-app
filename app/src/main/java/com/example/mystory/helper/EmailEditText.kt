package com.example.mystory.helper

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.example.mystory.R

class EmailEditText(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {
    init {
        inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        hint = context.getString(R.string.email)

        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    validateEmail()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    fun validateEmail(): Boolean {
        val email = text.toString().trim()
        return if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            error = null
            true
        } else {
            error = context.getString(R.string.email_error)
            false
        }
    }
}

