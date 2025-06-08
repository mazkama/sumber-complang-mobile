package com.febrivio.sumbercomplang.services

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveLogin(token: String, idUser: Int, name: String, email: String, role: String) {
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("auth_token", token)
            putInt("user_id", idUser)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_role", role)
            apply()
        }
    }

    fun saveUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }

    fun saveUserEmail(email: String) {
        prefs.edit().putString("user_email", email).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun getUserId(): Int {
        return prefs.getInt("user_id", -1)
    }

    fun getUserName(): String? {
        return prefs.getString("user_name", null)
    }

    fun getUserEmail(): String? {
        return prefs.getString("user_email", null)
    }

    fun getUserRole(): String? {
        return prefs.getString("user_role", null)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
