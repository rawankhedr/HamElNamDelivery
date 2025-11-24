package com.example.mytalabat.util

import android.util.Patterns

object ValidationUtil {

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidName(name: String): Boolean {
        return name.isNotEmpty() && name.length >= 2
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        return phone.isNotEmpty() && phone.length >= 10 && phone.all { it.isDigit() }
    }
}
