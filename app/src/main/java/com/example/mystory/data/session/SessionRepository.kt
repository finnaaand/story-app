package com.example.mystory.data.session

class SessionRepository(private val sessionManager: SessionManager) {

    fun saveUserSession(token: String) {
        sessionManager.saveSession(token)
    }

    fun isUserLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    fun clearUserSession() {
        sessionManager.clearSession()
    }

    fun getUserToken(): String? {
        return sessionManager.getToken()
    }
}
