package com.example.mystory.data.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        const val TOKEN_KEY = "token"
        const val IS_LOGGED_IN = "is_logged_in"
    }

    fun saveSession(token: String) {
        preferences.edit().apply {
            putString(TOKEN_KEY, token)
            putBoolean(IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getToken(): String? {
        return preferences.getString(TOKEN_KEY, null)
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(IS_LOGGED_IN, false) && !getToken().isNullOrEmpty()
    }

    fun clearSession() {
        preferences.edit().apply {
            remove(TOKEN_KEY)
            remove(IS_LOGGED_IN)
            apply()
        }
    }
}

