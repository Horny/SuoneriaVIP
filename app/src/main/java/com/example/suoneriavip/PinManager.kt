 
package com.example.suoneriavip

import android.content.Context
import android.content.SharedPreferences

class PinManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pin_prefs", Context.MODE_PRIVATE)

    fun getSavedPin(): String? {
        return prefs.getString("app_pin", null)
    }

    fun setPin(pin: String) {
        prefs.edit().putString("app_pin", pin).apply()
    }

    fun checkPin(input: String): Boolean {
        return input == getSavedPin()
    }
}
