package com.febrivio.sumbercomplang.services

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveLogin(token: String, idUser: Int, username: String, name: String, no_hp: String, role: String) {
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("auth_token", token)
            putInt("user_id", idUser)
            putString("user_username", username)
            putString("user_name", name)
            putString("user_no_hp", no_hp)
            putString("user_role", role)
            apply()
        }
    }

    fun saveUserUsername(username: String) {
        prefs.edit().putString("user_username", username).apply()
    }

    fun saveUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }

    fun saveUserNoHp(username: String) {
        prefs.edit().putString("user_no_hp", username).apply()
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

    fun getUserUsername(): String? {
        return prefs.getString("user_username", null)
    }

    fun getUserName(): String? {
        return prefs.getString("user_name", null)
    }

    fun getUserNoHp(): String? {
        return prefs.getString("user_no_hp", null)
    }

    fun getUserRole(): String? {
        return prefs.getString("user_role", null)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
